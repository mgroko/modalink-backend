package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.dto.InconsistenciaCronogramaResponse;
import org.mgroko.backend.admin.dto.RepararCronogramaRequest;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
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
 * Detección y reparación controlada de inconsistencias en cronogramas
 * confirmados. No se ejecuta automáticamente: solo desde el endpoint admin.
 */
@Service
public class RepararCronogramaService {

    private static final Logger LOG =
            LoggerFactory.getLogger(RepararCronogramaService.class);

    public static final String TIPO_ORIGEN_REAL_EN_PENDIENTE = "ORIGEN_REAL_EN_PENDIENTE";
    public static final String TIPO_INICIO_ANTES_DE_PREDECESORA = "INICIO_ANTES_DE_PREDECESORA";
    public static final String TIPO_SUCESORA_AVANZO_A_PREDECESORA = "SUCESORA_AVANZO_A_PREDECESORA";

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
                : proyectoRepository.findByEstado(EstadoProyecto.CONFIRMADO);

        int max = limite == null ? Integer.MAX_VALUE : Math.max(1, limite);
        List<InconsistenciaCronogramaResponse> resultado = new ArrayList<>();
        int procesados = 0;

        for (Proyecto proyecto : proyectos) {
            if (procesados >= max) {
                break;
            }
            if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
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

            for (Actividad actividad : actividades) {
                if (idActividad != null
                        && !idActividad.equals(actividad.getIdActividad())) {
                    continue;
                }
                InconsistenciaCronogramaResponse inconsistencia =
                        detectarActividad(proyecto, actividad);
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
                if (TIPO_SUCESORA_AVANZO_A_PREDECESORA.equals(inconsistencia.tipo())) {
                    fallidas++;
                    errores.add("Proyecto " + inconsistencia.idProyecto()
                            + ", actividad " + inconsistencia.idActividad()
                            + ": requiere intervención manual; no se mueven actividades en curso o finalizadas.");
                    continue;
                }

                try {
                    boolean tieneAsignaciones = !asignacionActividadRepository
                            .findActivasByActividadId(inconsistencia.idActividad())
                            .isEmpty();

                    reprogramarProyectoService.reprogramarActividad(
                            inconsistencia.idProyecto(),
                            inconsistencia.idActividad(),
                            MotivoReprogramacion.REPARACION,
                            Map.of(),
                            !tieneAsignaciones);
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
            Actividad actividad) {

        if (actividad.getEstado() == EstadoActividad.CANCELADA) {
            return null;
        }

        if (actividad.getEstado() == EstadoActividad.PENDIENTE
                && actividad.getOrigenInicio() == OrigenInicio.REAL) {
            return inconsistencia(
                    proyecto,
                    actividad,
                    TIPO_ORIGEN_REAL_EN_PENDIENTE,
                    "La actividad está PENDIENTE pero su origen de inicio es REAL.",
                    actividad.getFechaHoraInicio(),
                    null);
        }

        LocalDateTime finMaximoPredecesoras = finMaximoPredecesoras(actividad);

        if (actividad.getEstado() == EstadoActividad.PENDIENTE
                && actividad.getFechaHoraInicio() != null
                && finMaximoPredecesoras != null
                && actividad.getFechaHoraInicio().isBefore(finMaximoPredecesoras)) {
            return inconsistencia(
                    proyecto,
                    actividad,
                    TIPO_INICIO_ANTES_DE_PREDECESORA,
                    "La actividad comienza antes de que termine su predecesora más tardía.",
                    actividad.getFechaHoraInicio(),
                    finMaximoPredecesoras);
        }

        if (actividad.getEstado() == EstadoActividad.EN_CURSO
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            for (Actividad predecesora : actividad.getPredecesoras()) {
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
        }

        return null;
    }

    private LocalDateTime finMaximoPredecesoras(Actividad actividad) {
        LocalDateTime maximo = null;
        for (Actividad predecesora : actividad.getPredecesoras()) {
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
