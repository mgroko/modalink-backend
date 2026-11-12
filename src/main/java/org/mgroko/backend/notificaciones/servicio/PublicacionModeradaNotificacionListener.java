package org.mgroko.backend.notificaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.evento.PublicacionModeradaEvent;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * UC-69: notifica al dueño cuando el administrador oculta o reactiva una
 * publicación.
 *
 * <p>Se ejecuta después del commit para no notificar acciones revertidas. El
 * ocultamiento no lleva link; la reactivación sí, porque la publicación vuelve
 * a ser visible.</p>
 */
@Component
public class PublicacionModeradaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(PublicacionModeradaNotificacionListener.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public PublicacionModeradaNotificacionListener(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alModerarPublicacion(PublicacionModeradaEvent evento) {
        try {
            Usuario usuarioDestino = usuarioRepository
                    .findById(evento.idUsuarioDestino())
                    .orElse(null);

            if (usuarioDestino == null) {
                LOG.warn(
                        "Publicación {} moderada sin notificación: usuario {} no encontrado.",
                        evento.idPublicacion(),
                        evento.idUsuarioDestino());
                return;
            }

            Perfil perfilContexto = evento.idPerfil() != null
                    ? perfilRepository.findById(evento.idPerfil()).orElse(null)
                    : null;

            boolean oculta = evento.estado() == EstadoPublicacion.OCULTA;
            String tipo = oculta
                    ? NotificacionService.TIPO_PUBLICACION_OCULTADA
                    : NotificacionService.TIPO_PUBLICACION_REACTIVADA;
            String mensaje = construirMensaje(evento, oculta);

            Notificacion notificacion = notificacionService.crear(
                    usuarioDestino,
                    perfilContexto,
                    tipo,
                    mensaje,
                    NotificacionService.ENTIDAD_PUBLICACION,
                    evento.idPublicacion(),
                    null);

            notificacionService.registrarTodas(List.of(notificacion));
        } catch (RuntimeException ex) {
            LOG.warn(
                    "No se pudo registrar la notificación de moderación de la publicación {}.",
                    evento.idPublicacion(),
                    ex);
        }
    }

    private String construirMensaje(PublicacionModeradaEvent evento, boolean oculta) {
        String titulo = evento.titulo() != null ? evento.titulo() : "";
        if (oculta) {
            return "Se ocultó la publicación «" + titulo
                    + "» por no cumplir las normas de la comunidad.";
        }
        return "Se reactivó la publicación «" + titulo + "». Ya es visible nuevamente.";
    }
}
