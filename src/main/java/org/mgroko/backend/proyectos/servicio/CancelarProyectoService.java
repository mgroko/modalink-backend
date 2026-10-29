package org.mgroko.backend.proyectos.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CancelarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoCancelableException;
import org.mgroko.backend.proyectos.exception.MotivoCancelacionObligatorioException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-35: Cancelar proyecto. Cancela las actividades no finalizadas, rechaza
 * postulaciones e invitaciones pendientes y notifica al equipo y a los
 * destinatarios afectados.
 */
@Service
public class CancelarProyectoService {

    public static final String PERMISO_CANCELAR_PROYECTO = "CANCELAR_PROYECTO";

    private static final String ENTIDAD_PROYECTO = "PROYECTO";
    private static final String ENTIDAD_POSTULACION_GRAL = "POSTULACION_GRAL";
    private static final String ENTIDAD_POSTULACION_ACTIVIDAD = "POSTULACION_ACTIVIDAD";
    private static final String ENTIDAD_INVITACION_GRAL = "INVITACION_GRAL";
    private static final String ENTIDAD_INVITACION_ACTIVIDAD = "INVITACION_ACTIVIDAD";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ActividadRepository actividadRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final NotificacionService notificacionService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ObtenerProyectoService obtenerProyectoService;

    public CancelarProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ActividadRepository actividadRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            NotificacionService notificacionService,
            ProyectoSecurityService proyectoSecurityService,
            ObtenerProyectoService obtenerProyectoService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.actividadRepository = actividadRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.notificacionService = notificacionService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.obtenerProyectoService = obtenerProyectoService;
    }

    @Transactional
    public ProyectoResponse cancelar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CancelarProyectoRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(idProyecto, idPerfilActivo, PERMISO_CANCELAR_PROYECTO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoCancelableException(proyecto.getEstado().getNombre());
        }

        String motivo = normalizarMotivo(request);

        proyecto.setEstado(EstadoProyecto.CANCELADO);
        proyecto.setMotivoCancelacion(motivo);
        proyectoRepository.save(proyecto);

        actividadRepository.cancelarPendientesDeProyecto(idProyecto);

        List<PostulacionGral> postulacionesGral = rechazarPostulacionesGral(idProyecto);
        List<PostulacionActividad> postulacionesActividad = rechazarPostulacionesActividad(idProyecto);
        List<InvitacionGral> invitacionesGral = rechazarInvitacionesGral(idProyecto);
        List<InvitacionActividad> invitacionesActividad = rechazarInvitacionesActividad(idProyecto);

        List<Notificacion> notificaciones = new ArrayList<>();
        notificarEquipo(notificaciones, proyecto, motivo);
        notificarPostulacionesGral(notificaciones, proyecto, motivo, postulacionesGral);
        notificarPostulacionesActividad(notificaciones, proyecto, motivo, postulacionesActividad);
        notificarInvitacionesGral(notificaciones, proyecto, motivo, invitacionesGral);
        notificarInvitacionesActividad(notificaciones, proyecto, motivo, invitacionesActividad);

        notificacionService.registrarTodas(notificaciones);

        return obtenerProyectoService.obtener(idUsuario, idPerfilActivo, idProyecto);
    }

    private String normalizarMotivo(CancelarProyectoRequest request) {
        if (request == null || request.motivoCancelacion() == null
                || request.motivoCancelacion().isBlank()) {
            throw new MotivoCancelacionObligatorioException();
        }
        return request.motivoCancelacion().trim();
    }

    private List<PostulacionGral> rechazarPostulacionesGral(Long idProyecto) {
        List<PostulacionGral> postulaciones = postulacionGralRepository
                .findPendientesDeProyectoConPerfil(idProyecto);
        postulaciones.forEach(postulacion -> postulacion.setEstado(EstadoSolicitud.RECHAZADA));
        return postulacionGralRepository.saveAll(postulaciones);
    }

    private List<PostulacionActividad> rechazarPostulacionesActividad(Long idProyecto) {
        List<PostulacionActividad> postulaciones = postulacionActividadRepository
                .findPendientesDeProyectoConPerfil(idProyecto);
        postulaciones.forEach(postulacion -> postulacion.setEstado(EstadoSolicitud.RECHAZADA));
        return postulacionActividadRepository.saveAll(postulaciones);
    }

    private List<InvitacionGral> rechazarInvitacionesGral(Long idProyecto) {
        List<InvitacionGral> invitaciones = invitacionGralRepository
                .findPendientesDeProyectoConDestinatario(idProyecto);
        invitaciones.forEach(invitacion -> invitacion.setEstado(EstadoSolicitud.RECHAZADA));
        return invitacionGralRepository.saveAll(invitaciones);
    }

    private List<InvitacionActividad> rechazarInvitacionesActividad(Long idProyecto) {
        List<InvitacionActividad> invitaciones = invitacionActividadRepository
                .findPendientesDeProyectoConDestinatario(idProyecto);
        invitaciones.forEach(invitacion -> invitacion.setEstado(EstadoSolicitud.RECHAZADA));
        return invitacionActividadRepository.saveAll(invitaciones);
    }

    private void notificarEquipo(List<Notificacion> notificaciones, Proyecto proyecto, String motivo) {
        Set<Long> destinatarios = new HashSet<>();
        String mensaje = "El proyecto '" + proyecto.getNombre()
                + "' fue cancelado. Motivo: " + motivo;

        for (MiembroProyecto miembro : miembroProyectoRepository
                .buscarMiembrosActivosDeProyecto(proyecto.getIdProyecto())) {
            Perfil perfil = miembro.getPerfil();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (usuario != null && destinatarios.add(perfil.getIdPerfil())) {
                notificaciones.add(notificacionService.crear(
                        usuario,
                        perfil,
                        NotificacionService.TIPO_PROYECTO_CANCELADO,
                        mensaje,
                        ENTIDAD_PROYECTO,
                        proyecto.getIdProyecto(),
                        proyecto.getIdProyecto()));
            }
        }
    }

    private void notificarPostulacionesGral(
            List<Notificacion> notificaciones,
            Proyecto proyecto,
            String motivo,
            List<PostulacionGral> postulaciones) {

        String mensaje = "Tu postulación al proyecto '" + proyecto.getNombre()
                + "' fue rechazada porque el proyecto fue cancelado. Motivo: " + motivo;

        for (PostulacionGral postulacion : postulaciones) {
            Perfil perfil = postulacion.getPerfil();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (usuario != null) {
                notificaciones.add(notificacionService.crear(
                        usuario,
                        perfil,
                        NotificacionService.TIPO_POSTULACION_RECHAZADA,
                        mensaje,
                        ENTIDAD_POSTULACION_GRAL,
                        postulacion.getIdPostulacionGral(),
                        proyecto.getIdProyecto()));
            }
        }
    }

    private void notificarPostulacionesActividad(
            List<Notificacion> notificaciones,
            Proyecto proyecto,
            String motivo,
            List<PostulacionActividad> postulaciones) {

        String mensaje = "Tu postulación a una actividad del proyecto '" + proyecto.getNombre()
                + "' fue rechazada porque el proyecto fue cancelado. Motivo: " + motivo;

        for (PostulacionActividad postulacion : postulaciones) {
            Perfil perfil = postulacion.getPerfil();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (usuario != null) {
                notificaciones.add(notificacionService.crear(
                        usuario,
                        perfil,
                        NotificacionService.TIPO_POSTULACION_RECHAZADA,
                        mensaje,
                        ENTIDAD_POSTULACION_ACTIVIDAD,
                        postulacion.getIdPostulacion(),
                        proyecto.getIdProyecto()));
            }
        }
    }

    private void notificarInvitacionesGral(
            List<Notificacion> notificaciones,
            Proyecto proyecto,
            String motivo,
            List<InvitacionGral> invitaciones) {

        String mensaje = "Tu invitación al proyecto '" + proyecto.getNombre()
                + "' fue rechazada porque el proyecto fue cancelado. Motivo: " + motivo;

        for (InvitacionGral invitacion : invitaciones) {
            Perfil perfil = invitacion.getPerfilDestinatario();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (usuario != null) {
                notificaciones.add(notificacionService.crear(
                        usuario,
                        perfil,
                        NotificacionService.TIPO_INVITACION_RECHAZADA,
                        mensaje,
                        ENTIDAD_INVITACION_GRAL,
                        invitacion.getIdInvitacionGral(),
                        proyecto.getIdProyecto()));
            }
        }
    }

    private void notificarInvitacionesActividad(
            List<Notificacion> notificaciones,
            Proyecto proyecto,
            String motivo,
            List<InvitacionActividad> invitaciones) {

        String mensaje = "Tu invitación a una actividad del proyecto '" + proyecto.getNombre()
                + "' fue rechazada porque el proyecto fue cancelado. Motivo: " + motivo;

        for (InvitacionActividad invitacion : invitaciones) {
            Perfil perfil = invitacion.getPerfilDestinatario();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (usuario != null) {
                notificaciones.add(notificacionService.crear(
                        usuario,
                        perfil,
                        NotificacionService.TIPO_INVITACION_RECHAZADA,
                        mensaje,
                        ENTIDAD_INVITACION_ACTIVIDAD,
                        invitacion.getIdInvitacion(),
                        proyecto.getIdProyecto()));
            }
        }
    }
}
