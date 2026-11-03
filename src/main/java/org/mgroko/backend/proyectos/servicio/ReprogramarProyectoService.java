package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.proyectos.dto.ActividadReprogramadaResponse;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.dto.ConflictoFijoResponse;
import org.mgroko.backend.proyectos.dto.ReprogramacionProyectoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcula y persiste el cronograma de un proyecto confirmado.
 *
 * <p>Es la pieza común entre el cambio de fecha de entrega y la reprogramación
 * automática por cambio de jornada o margen. La operación es transaccional y
 * atómica por proyecto: si el planificador no encuentra un plan viable, no se
 * persiste ningún cambio parcial.</p>
 *
 * <p>Las actividades {@code PENDIENTE} se limpian y se vuelven a planificar.
 * Las {@code EN_CURSO} y {@code FINALIZADA} conservan su inicio real. Las
 * {@code CANCELADA} no se tocan.</p>
 */
@Service
public class ReprogramarProyectoService {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReprogramarProyectoService.class);

    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final PlanificadorRecursosService planificadorRecursosService;
    private final CalendarioService calendarioService;

    public ReprogramarProyectoService(
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            PlanificadorRecursosService planificadorRecursosService,
            CalendarioService calendarioService) {
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.planificadorRecursosService = planificadorRecursosService;
        this.calendarioService = calendarioService;
    }

    /**
     * Reprograma el proyecto completo.
     *
     * @param idProyecto         proyecto confirmado
     * @param motivo             origen de la reprogramación
     * @param nuevaFechaEntrega  nueva entrega, o {@code null} para conservarla
     * @return actividades movidas y conflictos de actividades fijas
     */
    @Transactional
    public ReprogramacionProyectoResponse reprogramar(
            Long idProyecto,
            MotivoReprogramacion motivo,
            LocalDate nuevaFechaEntrega) {

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ProyectoNoConfirmableException(
                    "ESTADO_PROYECTO_INVALIDO",
                    "Solo se puede reprogramar un proyecto confirmado. Estado actual: "
                            + (proyecto.getEstado() != null
                                    ? proyecto.getEstado().getNombre()
                                    : "desconocido")
                            + ".",
                    List.of(new BloqueoConfirmacionResponse(
                            "ESTADO_PROYECTO_INVALIDO",
                            "El proyecto no está confirmado.",
                            "PROYECTO",
                            idProyecto)));
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (nuevaFechaEntrega != null) {
            planificacion.setFechaEntrega(nuevaFechaEntrega);
            planificacionRepository.save(planificacion);
        }

        LocalDateTime fechaInicio = proyecto.getFechaInicio();
        if (fechaInicio == null) {
            throw new ProyectoNoConfirmableException(
                    "DATOS_INCONSISTENTES",
                    "El proyecto confirmado no tiene fecha de inicio.",
                    List.of(new BloqueoConfirmacionResponse(
                            "DATOS_INCONSISTENTES",
                            "Proyecto confirmado sin fecha de inicio.",
                            "PROYECTO",
                            idProyecto)));
        }

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(
                        planificacion.getIdPlanificacion());

        Map<Long, LocalDateTime> iniciosAnteriores = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() != EstadoActividad.CANCELADA) {
                iniciosAnteriores.put(
                        actividad.getIdActividad(),
                        actividad.getFechaHoraInicio());
            }
        }

        List<Actividad> pendientes = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.PENDIENTE)
                .toList();

        if (pendientes.isEmpty()) {
            return construirResultado(
                    proyecto,
                    planificacion,
                    motivo,
                    List.of(),
                    conflictosFijos(actividades));
        }

        // Las fechas viejas de las PENDIENTE no deben bloquear el recálculo como
        // si fueran reservas. La transacción las restaura si algo falla.
        for (Actividad actividad : pendientes) {
            actividad.setFechaHoraInicio(null);
            actividad.setOrigenInicio(null);
        }
        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        PlanificadorRecursosService.Resultado plan;
        try {
            plan = planificadorRecursosService.planificar(
                    planificacion,
                    actividades,
                    fechaInicio,
                    true);
        } catch (ProyectoNoConfirmableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo recalcular el cronograma del proyecto.",
                    List.of(new BloqueoConfirmacionResponse(
                            "CRONOGRAMA_NO_VIABLE",
                            "No se pudo recalcular el cronograma.",
                            "PROYECTO",
                            idProyecto)));
        }

        List<ActividadReprogramadaResponse> cambios = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }

            LocalDateTime inicio = plan.inicios().get(actividad.getIdActividad());
            if (inicio == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "No se pudo asignar un horario a la actividad '"
                                + actividad.getNombre() + "'.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Actividad sin horario luego de la reprogramación.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }

            LocalDateTime anterior = iniciosAnteriores.get(actividad.getIdActividad());
            actividad.setFechaHoraInicio(inicio);
            actividad.setOrigenInicio(
                    actividad.getEstado() == EstadoActividad.PENDIENTE
                            ? OrigenInicio.CONFIRMADA
                            : OrigenInicio.REAL);

            if (!Objects.equals(anterior, inicio)) {
                cambios.add(new ActividadReprogramadaResponse(
                        actividad.getIdActividad(),
                        actividad.getNombre(),
                        actividad.getEstado().name(),
                        anterior,
                        inicio));
            }
        }

        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        List<ConflictoFijoResponse> conflictos = conflictosFijos(actividades);

        LOG.info("Proyecto {} reprogramado por {}. Actividades movidas: {}. Conflictos fijos: {}.",
                idProyecto, motivo, cambios.size(), conflictos.size());

        return construirResultado(
                proyecto,
                planificacion,
                motivo,
                cambios,
                conflictos);
    }

    private ReprogramacionProyectoResponse construirResultado(
            Proyecto proyecto,
            Planificacion planificacion,
            MotivoReprogramacion motivo,
            List<ActividadReprogramadaResponse> cambios,
            List<ConflictoFijoResponse> conflictos) {

        return new ReprogramacionProyectoResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                motivo != null ? motivo.name() : null,
                planificacion.getFechaEntrega(),
                cambios,
                conflictos);
    }

    /**
     * Detecta actividades fijas que ya no entran en la nueva disponibilidad de
     * alguno de sus asignados. No las mueve: solo las reporta.
     */
    private List<ConflictoFijoResponse> conflictosFijos(List<Actividad> actividades) {
        List<Actividad> fijas = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.EN_CURSO
                        || actividad.getEstado() == EstadoActividad.FINALIZADA)
                .filter(actividad -> actividad.getFechaHoraInicio() != null)
                .toList();

        if (fijas.isEmpty()) {
            return List.of();
        }

        List<Long> idsFijas = fijas.stream()
                .map(Actividad::getIdActividad)
                .toList();

        Map<Long, List<AsignacionActividad>> asignacionesPorActividad = new HashMap<>();
        for (AsignacionActividad asignacion
                : asignacionActividadRepository.findActivasByActividadIds(idsFijas)) {
            if (asignacion.getActividad() == null
                    || asignacion.getActividad().getIdActividad() == null) {
                continue;
            }
            asignacionesPorActividad
                    .computeIfAbsent(
                            asignacion.getActividad().getIdActividad(),
                            id -> new ArrayList<>())
                    .add(asignacion);
        }

        List<ConflictoFijoResponse> conflictos = new ArrayList<>();
        for (Actividad fija : fijas) {
            List<Long> usuarios = usuariosDe(
                    asignacionesPorActividad.getOrDefault(
                            fija.getIdActividad(), List.of()));
            if (usuarios.isEmpty()) {
                continue;
            }

            try {
                boolean disponible = calendarioService.ventanaDisponiblePara(
                        usuarios,
                        fija.getFechaHoraInicio(),
                        fija.getFechaHoraFin(),
                        fija.getIdActividad());
                if (!disponible) {
                    conflictos.add(new ConflictoFijoResponse(
                            fija.getIdActividad(),
                            fija.getNombre(),
                            usuarios,
                            "La actividad " + fija.getEstado().name().toLowerCase()
                                    + " quedó fuera de la nueva disponibilidad y no se movió."));
                }
            } catch (RuntimeException ex) {
                conflictos.add(new ConflictoFijoResponse(
                        fija.getIdActividad(),
                        fija.getNombre(),
                        usuarios,
                        "No se pudo verificar la disponibilidad de la actividad fija: "
                                + ex.getMessage()));
            }
        }

        return conflictos;
    }

    private List<Long> usuariosDe(Collection<AsignacionActividad> asignaciones) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getMiembro() == null
                    || asignacion.getMiembro().getPerfil() == null
                    || asignacion.getMiembro().getPerfil().getUsuario() == null) {
                continue;
            }
            ids.add(asignacion.getMiembro().getPerfil().getUsuario().getIdUsuario());
        }
        return List.copyOf(ids);
    }
}
