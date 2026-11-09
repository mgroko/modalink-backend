package org.mgroko.backend.conexiones.servicio;

import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
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

    private final PerfilActivoService perfilActivoService;
    private final SolicitudConexionRepository solicitudConexionRepository;
    private final NotificacionService notificacionService;

    public CancelarSolicitudConexionService(
            PerfilActivoService perfilActivoService,
            SolicitudConexionRepository solicitudConexionRepository,
            NotificacionService notificacionService) {
        this.perfilActivoService = perfilActivoService;
        this.solicitudConexionRepository = solicitudConexionRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void cancelar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudConexion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

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
}
