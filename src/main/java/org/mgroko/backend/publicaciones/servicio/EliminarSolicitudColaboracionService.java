package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-64 - Eliminar una solicitud de colaboración enviada.
 *
 * <p>Sólo el perfil remitente puede eliminar una solicitud que siga en estado
 * {@code PENDIENTE}. La fila se elimina físicamente y se borra la notificación
 * de solicitud recibida del destinatario, de modo que la solicitud deje de ser
 * visible para él y el índice único parcial quede libre para reenviar.</p>
 */
@Service
public class EliminarSolicitudColaboracionService {

    private final PerfilActivoService perfilActivoService;
    private final SolicitudColaboracionRepository solicitudColaboracionRepository;
    private final NotificacionService notificacionService;

    public EliminarSolicitudColaboracionService(
            PerfilActivoService perfilActivoService,
            SolicitudColaboracionRepository solicitudColaboracionRepository,
            NotificacionService notificacionService) {
        this.perfilActivoService = perfilActivoService;
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudColaboracion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        SolicitudColaboracion solicitud = solicitudColaboracionRepository
                .findByIdForUpdate(idSolicitudColaboracion)
                .orElseThrow(SolicitudColaboracionNoEncontradaException::new);

        if (!solicitud.getPerfilRemitente().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())) {
            throw new SolicitudColaboracionNoPermitidaException(
                    "Solo el perfil remitente puede eliminar la solicitud de colaboración.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new SolicitudColaboracionNoPendienteException(
                    "Sólo se pueden eliminar solicitudes de colaboración pendientes.");
        }

        notificacionService.eliminarPorEntidadYPerfil(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                NotificacionService.ENTIDAD_PUBLICACION,
                solicitud.getPublicacion().getIdPublicacion(),
                solicitud.getPerfilDestinatario().getUsuario().getIdUsuario(),
                solicitud.getPerfilDestinatario().getIdPerfil());

        solicitudColaboracionRepository.delete(solicitud);
        solicitudColaboracionRepository.flush();
    }
}
