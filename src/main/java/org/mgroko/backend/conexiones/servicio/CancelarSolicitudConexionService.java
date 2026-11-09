package org.mgroko.backend.conexiones.servicio;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extensión de UC-43 - Cancelar una solicitud de conexión enviada.
 *
 * <p>Sólo el perfil remitente puede cancelar una solicitud que siga en estado
 * {@code PENDIENTE}. La fila se elimina físicamente y se borra la notificación
 * de solicitud recibida del destinatario, de modo que el par queda libre para
 * volver a enviar una solicitud.</p>
 */
@Service
public class CancelarSolicitudConexionService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SolicitudConexionRepository solicitudConexionRepository;
    private final NotificacionService notificacionService;

    public CancelarSolicitudConexionService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            SolicitudConexionRepository solicitudConexionRepository,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.solicitudConexionRepository = solicitudConexionRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void cancelar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudConexion) {

        Perfil perfilActivo = resolverPerfilActivo(idUsuario, idPerfilActivo);

        SolicitudConexion solicitud = solicitudConexionRepository
                .findByIdForUpdate(idSolicitudConexion)
                .orElseThrow(SolicitudConexionNoEncontradaException::new);

        if (!solicitud.getPerfilRemitente().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())) {
            throw new SolicitudConexionNoPermitidaException(
                    "La solicitud no pertenece al perfil activo.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new SolicitudConexionNoPendienteException(
                    "Sólo se pueden cancelar solicitudes de conexión pendientes.");
        }

        notificacionService.eliminarPorEntidad(
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                NotificacionService.ENTIDAD_SOLICITUD_CONEXION,
                idSolicitudConexion);

        solicitudConexionRepository.delete(solicitud);
        solicitudConexionRepository.flush();
    }

    private Perfil resolverPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        PerfilAccessPolicy.validarNoAdministrador(usuario);

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
            throw new PerfilEnBajaException(
                    "El usuario del perfil no se encuentra activo.");
        }

        return perfil;
    }
}
