package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.actividades.dto.DesasignacionActividadResponse;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoPermitidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Desasigna físicamente un miembro de una actividad.
 *
 * <p>La operación no requiere fecha de entrega ni disponibilidad: sólo valida
 * permisos, estados y pertenencia. Si la asignación cubría un requerimiento,
 * el cupo se libera al borrar la fila y se devuelve el estado resultante. Las
 * postulaciones rechazadas no se reabren automáticamente.</p>
 *
 * <p>La notificación {@code ACTIVIDAD_DESASIGNADA} se crea desde el servicio,
 * no desde un trigger, para no notificar cuando una actividad completa se
 * elimina y arrastra sus asignaciones.</p>
 */
@Service
public class DesasignarActividadService {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final ReprogramarProyectoService reprogramarProyectoService;

    public DesasignarActividadService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            ReprogramarProyectoService reprogramarProyectoService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.reprogramarProyectoService = reprogramarProyectoService;
    }

    @Transactional
    public DesasignacionActividadResponse desasignar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            Long idAsignacionActividad) {

        validarPerfilActor(idUsuario, idPerfilActivo);

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                AsignarActividadService.PERMISO_ASIGNAR_ACTIVIDAD);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);
        validarEstadoProyecto(proyecto);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        validarEstadoActividad(actividad);

        AsignacionActividad asignacion = asignacionActividadRepository
                .findByIdParaDesasignar(idAsignacionActividad)
                .orElseThrow(() -> new AsignacionActividadNoEncontradaException(
                        idAsignacionActividad));

        if (!asignacion.getActividad().getIdActividad().equals(idActividad)
                || !asignacion.getActividad().getPlanificacion().getProyecto()
                        .getIdProyecto().equals(idProyecto)) {
            throw new AsignacionActividadNoEncontradaException(idAsignacionActividad);
        }

        MiembroProyecto miembro = asignacion.getMiembro();
        if (miembro == null
                || miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO) {
            throw new AsignacionActividadNoPermitidaException(
                    "La asignación es histórica: el miembro ya no está activo en el proyecto.");
        }

        Perfil perfil = miembro.getPerfil();
        RequerimientoActividad requerimiento = asignacion.getRequerimientoActividad();

        asignacionActividadRepository.delete(asignacion);
        asignacionActividadRepository.flush();

        if (proyecto.getEstado() != null
                && proyecto.getEstado().permiteReprogramacion()
                && planificacion.getFechaEntrega() != null) {
            reprogramarProyectoService.reprogramar(
                    idProyecto,
                    MotivoReprogramacion.DESASIGNACION,
                    null);
        }

        CupoRequerimiento cupo = requerimiento != null
                ? cupoRequerimientoService.calcularActividad(requerimiento)
                : null;

        List<String> advertencias = construirAdvertencias(proyecto, cupo);
        notificarDesasignacion(proyecto, actividad, perfil);

        return new DesasignacionActividadResponse(
                idAsignacionActividad,
                proyecto.getIdProyecto(),
                actividad.getIdActividad(),
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null && perfil.getUsuario() != null
                        ? perfil.getUsuario().getIdUsuario()
                        : null,
                requerimiento != null ? requerimiento.getIdRequerimiento() : null,
                cupo != null && cupo.satisfecho(),
                cupo,
                advertencias);
    }

    private void validarPerfilActor(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
            throw new AsignacionActividadNoPermitidaException(
                    "El proyecto no admite desasignaciones en estado "
                            + estado.getNombre() + ".");
        }
    }

    private void validarEstadoActividad(Actividad actividad) {
        if (actividad.getEstado() == EstadoActividad.CANCELADA
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            throw new AsignacionActividadNoPermitidaException(
                    "La actividad no admite desasignaciones en estado "
                            + actividad.getEstado().name() + ".");
        }
    }

    private List<String> construirAdvertencias(
            Proyecto proyecto,
            CupoRequerimiento cupo) {

        List<String> advertencias = new ArrayList<>();
        if (cupo != null && !cupo.satisfecho()) {
            advertencias.add("El requerimiento quedó con "
                    + cupo.faltantes() + " vacante(s).");
        }
        return advertencias;
    }

    private void notificarDesasignacion(
            Proyecto proyecto,
            Actividad actividad,
            Perfil perfil) {

        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                perfil.getUsuario(),
                perfil,
                NotificacionService.TIPO_ACTIVIDAD_DESASIGNADA,
                "Fuiste desasignado de la actividad '" + actividad.getNombre()
                        + "' del proyecto '" + proyecto.getNombre() + "'.",
                NotificacionDestinoResolver.ENTIDAD_ACTIVIDAD,
                actividad.getIdActividad(),
                proyecto.getIdProyecto());

        notificacionService.registrarTodas(List.of(notificacion));
    }
}
