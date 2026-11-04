package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.InconsistenciaCronogramaResponse;
import org.mgroko.backend.admin.dto.RepararCronogramaRequest;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detección y reparación controlada de inconsistencias en cronogramas de
 * proyectos publicados o confirmados.
 *
 * <p>La detección es de solo lectura. La reparación solo mueve actividades
 * {@code PENDIENTE}; las actividades {@code EN_CURSO} y {@code FINALIZADA} se
 * reportan para intervención manual. El servicio puede ejecutarse desde el
 * endpoint admin o desde el job periódico de reconciliación.</p>
 */
@Service
public class RepararCronogramaService {

    private static final Logger LOG =
            LoggerFactory.getLogger(RepararCronogramaService.class);

    private static final List<EstadoProyecto> ESTADOS_PROGRAMABLES =
            List.of(EstadoProyecto.PUBLICADO, EstadoProyecto.CONFIRMADO);

    public static final String TIPO_INICIO_ANTES_DE_PREDECESORA =
            "INICIO_ANTES_DE_PREDECESORA";
    public static final String TIPO_SUCESORA_AVANZO_A_PREDECESORA =
            "SUCESORA_AVANZO_A_PREDECESORA";
    public static final String TIPO_INICIO_ANTES_DE_PROYECTO =
            "INICIO_ANTES_DE_PROYECTO";
    public static final String TIPO_ACTIVIDAD_EJECUTADA_SIN_INICIO =
            "ACTIVIDAD_EJECUTADA_SIN_INICIO";
    public static final String TIPO_PENDIENTE_SIN_INICIO_CON_ASIGNACIONES =
            "PENDIENTE_SIN_INICIO_CON_ASIGNACIONES";

    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ReprogramarProyectoService reprogramarProyectoService;

    public RepararCronogramaService(
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            ReprogramarProyectoService reprogramarProyectoService) {
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.reprogramarProyectoService = reprogramarProyectoService;
    }

    @Transactional(readOnly = true)
    public List<InconsistenciaCronogramaResponse> detectar(
            Long idProyecto,
            Long idActividad,
            Integer limite) {

        List<Proyecto> proyectos = idProyecto != null
                ? proyectoRepository.findById(idProyecto)
                        .map(List::of)
                        .orElse(List.of())
                : proyectoRepository.findByEstadoIn(ESTADOS_PROGRAMABLES);

        int max = limite == null ? Integer.MAX_VALUE : Math.max(1, limite);
        List<InconsistenciaCronogramaResponse> resultado = new ArrayList<>();
        int procesados = 0;

        for (Proyecto proyecto : proyectos) {
            if (procesados >= max) {
                break;
            }
            if (proyecto.getEstado() == null
                    || !proyecto.getEstado().permiteReprogramacion()) {
                continue;
            }
            procesados++;

            Planificacion planificacion = planificacionRepository
                    .findByProyectoIdProyecto(proyecto.getIdProyecto())
                    .orElse(null);
            if (planificacion == null) {
                continue;
            }

            List<Actividad> actividades = actividadRepository
                    .findConPredecesorasByPlanificacion(
                            planificacion.getIdPlanificacion());
            Set<Long> conAsignacionesActivas = proyecto.getEstado()
                    == EstadoProyecto.CONFIRMADO
                            ? idsConAsignacionesActivas(actividades)
                            : Set.of();

            for (Actividad actividad : actividades) {
                if (idActividad != null
                        && !idActividad.equals(actividad.getIdActividad())) {
                    continue;
                }
                InconsistenciaCronogramaResponse inconsistencia =
                        detectarActividad(
                                proyecto, actividad, conAsignacionesActivas);
                if (inconsistencia != null) {
                    resultado.add(inconsistencia);
                }
            }
        }

        return resultado;
    }

    public ReparacionCronogramaResponse reparar(RepararCronogramaRequest request) {
        List<InconsistenciaCronogramaResponse> detectadas = detectar(
                request.idProyecto(),
                request.idActividad(),
                request.limite());

        if (request.esDryRun()) {
            return new ReparacionCronogramaResponse(
                    true, detectadas.size(), 0, 0, detectadas, List.of());
        }

        Map<Long, List<InconsistenciaCronogramaResponse>> porProyecto =
                detectadas.stream().collect(Collectors.groupingBy(
                        InconsistenciaCronogramaResponse::idProyecto,
                        LinkedHashMap::new,
                        Collectors.toList()));

        int reparadas = 0;
        int fallidas = 0;
        List<String> errores = new ArrayList<>();

        for (Map.Entry<Long, List<InconsistenciaCronogramaResponse>> entry
                : porProyecto.entrySet()) {
            for (InconsistenciaCronogramaResponse inconsistencia : entry.getValue()) {
                if (!esReparable(inconsistencia.tipo())) {
                    fallidas++;
                    errores.add(mensajeNoReparable(inconsistencia));
                    continue;
                }

                try {
                    reprogramarProyectoService.reprogramarActividad(
                            inconsistencia.idProyecto(),
                            inconsistencia.idActividad(),
                            MotivoReprogramacion.REPARACION,
                            Map.of());
                    reparadas++;
                } catch (RuntimeException ex) {
                    fallidas++;
                    errores.add("Proyecto " + inconsistencia.idProyecto()
                            + ", actividad " + inconsistencia.idActividad()
                            + ": " + (ex.getMessage() != null
                                    ? ex.getMessage()
                                    : ex.getClass().getSimpleName()));
                    LOG.warn("No se pudo reparar el cronograma del proyecto {} (actividad {}).",
                            inconsistencia.idProyecto(),
                            inconsistencia.idActividad(),
                            ex);
                }
            }
        }

        return new ReparacionCronogramaResponse(
                false,
                detectadas.size(),
                reparadas,
                fallidas,
                detectadas,
                errores);
    }

    private InconsistenciaCronogramaResponse detectarActividad(
            Proyecto proyecto,
            Actividad actividad,
            Set<Long> conAsignacionesActivas) {

        if (actividad == null
                || actividad.getEstado() == null
                || actividad.getEstado() == EstadoActividad.CANCELADA) {
            return null;
        }

        if (actividad.getEstado() == EstadoActividad.EN_CURSO
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            return detectarEjecutada(proyecto, actividad);
        }

        if (actividad.getEstado() != EstadoActividad.PENDIENTE) {
            return null;
        }

        return detectarPendiente(
                proyecto, actividad, conAsignacionesActivas);
    }

    /**
     * Una actividad ya ejecutada no puede repararse automáticamente: su inicio
     * real no se puede reconstruir. Si no lo tiene, se reporta.
     */
    private InconsistenciaCronogramaResponse detectarEjecutada(
            Proyecto proyecto,
            Actividad actividad) {

        if (actividad.getFechaHoraInicio() == null) {
            return inconsistencia(
                    proyecto,
                    actividad,
                    TIPO_ACTIVIDAD_EJECUTADA_SIN_INICIO,
                    "La actividad está " + actividad.getEstado()
                            + " pero no tiene inicio registrado.",
                    null,
                    null);
        }

        for (Actividad predecesora : predecesoras(actividad)) {
            if (predecesora == null
                    || predecesora.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            if (predecesora.getEstado() != EstadoActividad.FINALIZADA) {
                return inconsistencia(
                        proyecto,
                        actividad,
                        TIPO_SUCESORA_AVANZO_A_PREDECESORA,
                        "La actividad está " + actividad.getEstado()
                                + " pero su predecesora "
                                + predecesora.getIdActividad()
                                + " está " + predecesora.getEstado() + ".",
                        actividad.getFechaHoraInicio(),
                        predecesora.getFechaHoraFin());
            }
        }

        return null;
    }

    private InconsistenciaCronogramaResponse detectarPendiente(
            Proyecto proyecto,
            Actividad actividad,
            Set<Long> conAsignacionesActivas) {

        if (actividad.getFechaHoraInicio() == null) {
            if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO
                    && conAsignacionesActivas.contains(actividad.getIdActividad())) {
                return inconsistencia(
                        proyecto,
                        actividad,
                        TIPO_PENDIENTE_SIN_INICIO_CON_ASIGNACIONES,
                        "La actividad tiene asignaciones activas pero no tiene "
                                + "inicio planificado.",
                        null,
                        null);
            }
            return null;
        }

        if (proyecto.getFechaInicio() != null
                && actividad.getFechaHoraInicio()
                        .isBefore(proyecto.getFechaInicio())) {
            return inconsistencia(
                    proyecto,
                    actividad,
                    TIPO_INICIO_ANTES_DE_PROYECTO,
                    "La actividad comienza antes del inicio del proyecto.",
                    actividad.getFechaHoraInicio(),
                    proyecto.getFechaInicio());
        }

        LocalDateTime finMaximoPredecesoras = finMaximoPredecesoras(actividad);
        if (finMaximoPredecesoras != null
                && actividad.getFechaHoraInicio()
                        .isBefore(finMaximoPredecesoras)) {
            return inconsistencia(
                    proyecto,
                    actividad,
                    TIPO_INICIO_ANTES_DE_PREDECESORA,
                    "La actividad comienza antes de que termine su predecesora más tardía.",
                    actividad.getFechaHoraInicio(),
                    finMaximoPredecesoras);
        }

        return null;
    }

    private Set<Long> idsConAsignacionesActivas(List<Actividad> actividades) {
        Set<Long> ids = actividades.stream()
                .filter(Objects::nonNull)
                .filter(actividad -> actividad.getEstado() == EstadoActividad.PENDIENTE)
                .filter(actividad -> actividad.getFechaHoraInicio() == null)
                .map(Actividad::getIdActividad)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Set.of();
        }

        Set<Long> resultado = asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(ids);
        return resultado == null ? Set.of() : resultado;
    }

    private boolean esReparable(String tipo) {
        return TIPO_INICIO_ANTES_DE_PREDECESORA.equals(tipo)
                || TIPO_INICIO_ANTES_DE_PROYECTO.equals(tipo)
                || TIPO_PENDIENTE_SIN_INICIO_CON_ASIGNACIONES.equals(tipo);
    }

    private String mensajeNoReparable(InconsistenciaCronogramaResponse inconsistencia) {
        String prefijo = "Proyecto " + inconsistencia.idProyecto()
                + ", actividad " + inconsistencia.idActividad() + ": ";

        if (TIPO_SUCESORA_AVANZO_A_PREDECESORA.equals(inconsistencia.tipo())) {
            return prefijo
                    + "requiere intervención manual; no se mueven actividades en curso o finalizadas.";
        }
        if (TIPO_ACTIVIDAD_EJECUTADA_SIN_INICIO.equals(inconsistencia.tipo())) {
            return prefijo
                    + "requiere intervención manual; la actividad ya está en curso o finalizada y no tiene inicio registrado.";
        }
        return prefijo + "requiere intervención manual. " + inconsistencia.detalle();
    }

    private List<Actividad> predecesoras(Actividad actividad) {
        return actividad.getPredecesoras() == null
                ? List.of()
                : new ArrayList<>(actividad.getPredecesoras());
    }

    private LocalDateTime finMaximoPredecesoras(Actividad actividad) {
        LocalDateTime maximo = null;
        for (Actividad predecesora : predecesoras(actividad)) {
            if (predecesora == null
                    || predecesora.getEstado() == EstadoActividad.CANCELADA
                    || predecesora.getFechaHoraFin() == null) {
                continue;
            }
            if (maximo == null || predecesora.getFechaHoraFin().isAfter(maximo)) {
                maximo = predecesora.getFechaHoraFin();
            }
        }
        return maximo;
    }

    private InconsistenciaCronogramaResponse inconsistencia(
            Proyecto proyecto,
            Actividad actividad,
            String tipo,
            String detalle,
            LocalDateTime inicioActual,
            LocalDateTime finMaximoPredecesoras) {

        return new InconsistenciaCronogramaResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                actividad.getIdActividad(),
                actividad.getNombre(),
                tipo,
                detalle,
                inicioActual,
                finMaximoPredecesoras);
    }
}
