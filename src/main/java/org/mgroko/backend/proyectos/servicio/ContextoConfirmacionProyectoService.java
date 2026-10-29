package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y valida el contexto común de UC-60.
 *
 * <p>Cuando {@code bloquear} es {@code true} toma locks pesimistas de proyecto
 * y planificación. El servicio de confirmación lo usa con lock; la
 * previsualización, sin lock.</p>
 */
@Service
public class ContextoConfirmacionProyectoService {

    public static final String PERMISO_CONFIRMAR_PROYECTO = "CONFIRMAR_PROYECTO";

    private static final LocalTime HORA_FIN_ENTREGA = LocalTime.of(23, 59);

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoCompletitudService requerimientoCompletitudService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final Clock reloj;

    public ContextoConfirmacionProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoCompletitudService requerimientoCompletitudService,
            ProyectoSecurityService proyectoSecurityService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoCompletitudService = requerimientoCompletitudService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Contexto cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            boolean bloquear) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = (bloquear
                ? proyectoRepository.findByIdParaGestion(idProyecto)
                : proyectoRepository.findById(idProyecto))
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_CONFIRMAR_PROYECTO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        List<BloqueoConfirmacionResponse> bloqueos = new ArrayList<>();

        if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "PROYECTO_YA_CONFIRMADO",
                    "El proyecto ya está confirmado.",
                    "PROYECTO",
                    idProyecto));
        } else if (proyecto.getEstado() != EstadoProyecto.PUBLICADO) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "ESTADO_PROYECTO_INVALIDO",
                    "Solo se puede confirmar un proyecto en estado Publicado. Estado actual: "
                            + proyecto.getEstado().getNombre() + ".",
                    "PROYECTO",
                    idProyecto));
        }

        Planificacion planificacion = (bloquear
                ? planificacionRepository.findByProyectoIdProyectoForUpdate(idProyecto)
                : planificacionRepository.findByProyectoIdProyecto(idProyecto))
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);

        if (planificacion.getFechaEntrega() == null) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "FECHA_ENTREGA_INVALIDA",
                    "La planificación no tiene fecha de entrega.",
                    "PROYECTO",
                    idProyecto));
        } else if (!planificacion.getFechaEntrega()
                .atTime(HORA_FIN_ENTREGA).isAfter(ahora)) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "FECHA_ENTREGA_INVALIDA",
                    "La fecha de entrega debe ser posterior a la fecha de inicio confirmada.",
                    "PROYECTO",
                    idProyecto));
        }

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
        List<Actividad> actividadesNoCanceladas = actividades.stream()
                .filter(actividad -> actividad.getEstado() != EstadoActividad.CANCELADA)
                .toList();

        if (actividadesNoCanceladas.isEmpty()) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "SIN_ACTIVIDADES",
                    "El proyecto no tiene actividades en la planificación.",
                    "PROYECTO",
                    idProyecto));
        }

        List<Long> idsActividades = actividadesNoCanceladas.stream()
                .map(Actividad::getIdActividad)
                .toList();

        RequerimientoCompletitudService.Resultado requerimientos =
                requerimientoCompletitudService.validar(idProyecto, idsActividades);
        bloqueos.addAll(requerimientos.bloqueos());

        List<AsignacionActividad> asignaciones = idsActividades.isEmpty()
                ? List.of()
                : asignacionActividadRepository.findActivasByActividadIds(idsActividades);
        Map<Long, List<AsignacionActividad>> asignacionesPorActividad = new HashMap<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getActividad() == null
                    || asignacion.getActividad().getIdActividad() == null) {
                continue;
            }
            asignacionesPorActividad
                    .computeIfAbsent(asignacion.getActividad().getIdActividad(),
                            id -> new ArrayList<>())
                    .add(asignacion);
        }

        Map<Long, Set<Long>> requerimientosPorActividad = new HashMap<>();
        if (!idsActividades.isEmpty()) {
            for (RequerimientoActividad requerimiento
                    : requerimientoActividadRepository.findConProfesionByActividadIds(idsActividades)) {
                if (requerimiento.getActividad() == null
                        || requerimiento.getActividad().getIdActividad() == null) {
                    continue;
                }
                requerimientosPorActividad
                        .computeIfAbsent(
                                requerimiento.getActividad().getIdActividad(),
                                id -> new HashSet<>())
                        .add(requerimiento.getIdRequerimiento());
            }
        }

        for (Actividad actividad : actividadesNoCanceladas) {
            List<AsignacionActividad> asignacionesActividad =
                    asignacionesPorActividad.getOrDefault(
                            actividad.getIdActividad(), List.of());
            if (asignacionesActividad.isEmpty()) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "ACTIVIDADES_SIN_ASIGNACION",
                        "La actividad '" + actividad.getNombre()
                                + "' no tiene integrantes activos asignados.",
                        "ACTIVIDAD",
                        actividad.getIdActividad()));
            }

            Set<Long> requerimientosActividad =
                    requerimientosPorActividad.getOrDefault(
                            actividad.getIdActividad(), Set.of());
            for (AsignacionActividad asignacion : asignacionesActividad) {
                if (asignacion.getRequerimientoActividad() == null) {
                    if (!requerimientosActividad.isEmpty()) {
                        bloqueos.add(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Una asignación de la actividad '" + actividad.getNombre()
                                        + "' no está vinculada a un requerimiento.",
                                "ACTIVIDAD",
                                actividad.getIdActividad()));
                    }
                    continue;
                }
                Long idRequerimiento = asignacion.getRequerimientoActividad()
                        .getIdRequerimiento();
                if (idRequerimiento == null
                        || !requerimientosActividad.contains(idRequerimiento)) {
                    bloqueos.add(new BloqueoConfirmacionResponse(
                            "DATOS_INCONSISTENTES",
                            "Una asignación de la actividad '" + actividad.getNombre()
                                    + "' apunta a un requerimiento de otra actividad.",
                            "ACTIVIDAD",
                            actividad.getIdActividad()));
                }
            }

            if ((actividad.getEstado() == EstadoActividad.EN_CURSO
                    || actividad.getEstado() == EstadoActividad.FINALIZADA)
                    && actividad.getFechaHoraInicio() == null) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "DATOS_INCONSISTENTES",
                        "La actividad '" + actividad.getNombre()
                                + "' está en ejecución pero no tiene fecha de inicio.",
                        "ACTIVIDAD",
                        actividad.getIdActividad()));
            }
        }

        return new Contexto(
                proyecto,
                planificacion,
                ahora,
                actividades,
                actividadesNoCanceladas,
                requerimientos,
                asignaciones,
                List.copyOf(bloqueos));
    }

    public record Contexto(
            Proyecto proyecto,
            Planificacion planificacion,
            LocalDateTime fechaInicioPropuesta,
            List<Actividad> actividades,
            List<Actividad> actividadesNoCanceladas,
            RequerimientoCompletitudService.Resultado requerimientos,
            List<AsignacionActividad> asignaciones,
            List<BloqueoConfirmacionResponse> bloqueos) {
    }
}
