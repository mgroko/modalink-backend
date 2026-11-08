package org.mgroko.backend.invitaciones.servicio;

import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-32: baja física de invitaciones pendientes.
 *
 * <p>Sólo el Director que envió la invitación puede eliminarla. La operación
 * bloquea la fila para evitar que una aceptación o rechazo concurrente deje el
 * estado inconsistente.</p>
 */
@Service
public class EliminarInvitacionService {

    public static final String CODIGO_INVITACION_NO_PENDIENTE = "INVITACION_NO_PENDIENTE";

    private final PerfilRepository perfilRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;

    public EliminarInvitacionService(
            PerfilRepository perfilRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    @AuditarOperacion
    public void eliminarInvitacionGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idInvitacion) {

        perfilActivo(idUsuario, idPerfilActivo);

        InvitacionGral invitacion = invitacionGralRepository
                .findByIdForUpdate(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        if (!invitacion.getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new InvitacionNoEncontradaException();
        }

        validarPermiso(idProyecto, idPerfilActivo);
        validarRemitente(invitacion.getPerfilRemitente(), idPerfilActivo);
        validarPendiente(invitacion.getEstado());

        notificacionService.eliminarPorEntidad(
                NotificacionService.TIPO_INVITACION_RECIBIDA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                idInvitacion);

        invitacionGralRepository.delete(invitacion);
        invitacionGralRepository.flush();
    }

    @Transactional
    @AuditarOperacion
    public void eliminarInvitacionActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idInvitacion) {

        perfilActivo(idUsuario, idPerfilActivo);

        InvitacionActividad invitacion = invitacionActividadRepository
                .findByIdForUpdate(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        Long idProyectoInvitacion = invitacion.getRequerimiento()
                .getActividad()
                .getPlanificacion()
                .getProyecto()
                .getIdProyecto();

        if (!idProyectoInvitacion.equals(idProyecto)) {
            throw new InvitacionNoEncontradaException();
        }

        validarPermiso(idProyecto, idPerfilActivo);
        validarRemitente(invitacion.getPerfilRemitente(), idPerfilActivo);
        validarPendiente(invitacion.getEstado());

        notificacionService.eliminarPorEntidad(
                NotificacionService.TIPO_INVITACION_RECIBIDA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                idInvitacion);

        invitacionActividadRepository.delete(invitacion);
        invitacionActividadRepository.flush();
    }

    private Perfil perfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException("El usuario del perfil no se encuentra activo.");
        }

        return perfil;
    }

    private void validarPermiso(Long idProyecto, Long idPerfilActivo) {
        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo,
                ProyectoSecurityService.PERMISO_ELIMINAR_INVITACION);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);
    }

    private void validarRemitente(Perfil remitente, Long idPerfilActivo) {
        if (remitente == null
                || remitente.getIdPerfil() == null
                || !remitente.getIdPerfil().equals(idPerfilActivo)) {
            throw new AccesoDenegadoProyectoException(
                    "Sólo el Director que envió la invitación puede eliminarla.");
        }
    }

    private void validarPendiente(EstadoSolicitud estado) {
        if (estado != EstadoSolicitud.PENDIENTE) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se pueden eliminar invitaciones pendientes.",
                    CODIGO_INVITACION_NO_PENDIENTE);
        }
    }
}
