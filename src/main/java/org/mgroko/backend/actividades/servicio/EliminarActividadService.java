package org.mgroko.backend.actividades.servicio;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.InvitacionAfectada;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.PostulacionAfectada;
import org.mgroko.backend.actividades.exception.ActividadNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteEliminarActividadesException;
import org.mgroko.backend.actividades.exception.MotivoEliminacionObligatorioException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarActividadService {

    public static final String PERMISO_ELIMINAR_ACTIVIDAD = "ELIMINAR_ACTIVIDAD";

    private static final int MOTIVO_MAX_LENGTH = 200;

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final RequerimientoActividadService requerimientoActividadService;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final CronogramaService cronogramaService;
    private final ApplicationEventPublisher eventPublisher;

    public EliminarActividadService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            RequerimientoActividadService requerimientoActividadService,
            RequerimientoActividadRepository requerimientoActividadRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            CronogramaService cronogramaService,
            ApplicationEventPublisher eventPublisher) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.requerimientoActividadService = requerimientoActividadService;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.cronogramaService = cronogramaService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            String motivo) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Proyecto proyecto = planificacion.getProyecto();
        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_ELIMINAR_ACTIVIDAD);
        validarEstadoProyecto(proyecto);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        validarEstadoActividad(actividad);
        String motivoNormalizado = normalizarMotivo(motivo);

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findByActividadIdConMiembroPerfil(idActividad);
        List<PostulacionActividad> postulaciones = postulacionActividadRepository
                .findByActividadIdConPerfil(idActividad);
        List<InvitacionActividad> invitaciones = invitacionActividadRepository
                .findByActividadIdConDestinatario(idActividad);

        ActividadEliminadaEvent evento = new ActividadEliminadaEvent(
                idActividad,
                actividad.getNombre(),
                proyecto.getNombre(),
                motivoNormalizado,
                perfilesAsignados(asignaciones),
                postulacionesAfectadas(postulaciones),
                invitacionesAfectadas(invitaciones));

        ActividadDependencias.reconectarSucesoras(actividadRepository, actividad, actividades);
        actividadRepository.deleteDependenciasByActividadId(idActividad);
        actividadRepository.deleteHitoActividadByActividadId(idActividad);
        postulaciones.forEach(postulacionActividadRepository::delete);
        invitaciones.forEach(invitacionActividadRepository::delete);
        eliminarRequerimientos(idActividad);
        asignaciones.forEach(asignacionActividadRepository::delete);
        actividadRepository.delete(actividad);
        actividadRepository.flush();
        // Fase 2: no persistir proyecciones al eliminar; el cronograma se mantiene
        // con los estados actuales de las actividades restantes.
        // Se omite el recálculo para evitar escribir fechaHoraInicio en PENDIENTE.
        publicarEvento(evento);
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO) {
            throw new EstadoProyectoNoPermiteEliminarActividadesException(estado.name());
        }
    }

    private void validarEstadoActividad(Actividad actividad) {
        EstadoActividad estado = actividad.getEstado();
        if (estado == EstadoActividad.FINALIZADA
                || estado == EstadoActividad.CANCELADA) {
            throw new ActividadNoEliminableException(estado.name());
        }
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new MotivoEliminacionObligatorioException();
        }

        String normalizado = motivo.trim();
        if (normalizado.length() > MOTIVO_MAX_LENGTH) {
            throw new MotivoEliminacionObligatorioException(
                    "El motivo de eliminación no puede superar los "
                            + MOTIVO_MAX_LENGTH + " caracteres.");
        }

        return normalizado;
    }

    private List<Long> perfilesAsignados(List<AsignacionActividad> asignaciones) {
        Set<Long> idsPerfiles = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getMiembro().getEstadoParticipacion()
                    == EstadoParticipacion.ACTIVO) {
                idsPerfiles.add(asignacion.getMiembro().getPerfil().getIdPerfil());
            }
        }
        return List.copyOf(idsPerfiles);
    }

    private List<PostulacionAfectada> postulacionesAfectadas(
            List<PostulacionActividad> postulaciones) {

        Map<Long, Long> idsPorPerfil = new LinkedHashMap<>();
        for (PostulacionActividad postulacion : postulaciones) {
            if (postulacion.getEstado() == EstadoSolicitud.RECHAZADA) {
                continue;
            }
            idsPorPerfil.putIfAbsent(
                    postulacion.getPerfil().getIdPerfil(),
                    postulacion.getIdPostulacion());
        }

        return idsPorPerfil.entrySet().stream()
                .map(entry -> new PostulacionAfectada(entry.getValue(), entry.getKey()))
                .toList();
    }

    private List<InvitacionAfectada> invitacionesAfectadas(
            List<InvitacionActividad> invitaciones) {

        Map<Long, Long> idsPorPerfil = new LinkedHashMap<>();
        for (InvitacionActividad invitacion : invitaciones) {
            if (invitacion.getEstado() == EstadoSolicitud.RECHAZADA) {
                continue;
            }
            idsPorPerfil.putIfAbsent(
                    invitacion.getPerfilDestinatario().getIdPerfil(),
                    invitacion.getIdInvitacion());
        }

        return idsPorPerfil.entrySet().stream()
                .map(entry -> new InvitacionAfectada(entry.getValue(), entry.getKey()))
                .toList();
    }

    private void eliminarRequerimientos(Long idActividad) {
        List<RequerimientoActividad> requerimientos = requerimientoActividadRepository
                .findByActividad_IdActividad(idActividad);

        for (RequerimientoActividad requerimiento : requerimientos) {
            requerimientoActividadService.eliminar(requerimiento);
        }

        if (!requerimientos.isEmpty()) {
            requerimientoActividadRepository.flush();
        }
    }

    private void recalcularCronograma(Planificacion planificacion) {

        if (planificacion.getFechaEntrega() == null) {
            return;
        }

        List<Actividad> restantes = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());

        if (restantes.isEmpty()) {
            return;
        }

        cronogramaService.recalcularTolerandoDesborde(planificacion, restantes);
    }

    private void publicarEvento(ActividadEliminadaEvent evento) {
        if (evento.idsPerfilesAsignados().isEmpty()
                && evento.postulaciones().isEmpty()
                && evento.invitaciones().isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(evento);
    }
}
