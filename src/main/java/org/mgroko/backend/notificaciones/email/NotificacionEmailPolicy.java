package org.mgroko.backend.notificaciones.email;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.springframework.stereotype.Component;

/**
 * Política global de envío de emails.
 *
 * <p>En esta versión no hay preferencias por usuario: se envía únicamente a
 * notificaciones cuyo tipo está activo con prioridad ALTA. La decisión se
 * refuerza en el trigger de outbox y se vuelve a validar al momento del envío
 * por si el usuario o el tipo cambiaron de estado.</p>
 */
@Component
public class NotificacionEmailPolicy {

    private final NotificacionRepository notificacionRepository;

    public NotificacionEmailPolicy(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Devuelve {@code null} si la notificación es elegible o el motivo por el
     * que no debe enviarse.
     */
    public String motivoNoElegible(Notificacion notificacion, Usuario usuario) {
        if (notificacion == null || usuario == null) {
            return "NOTIFICACION_O_USUARIO_INEXISTENTE";
        }
        if (notificacion.getIdNotificacion() == null || notificacion.getTipo() == null) {
            return "NOTIFICACION_INVALIDA";
        }
        if (!usuario.getEstado().permiteAcceso()) {
            return "USUARIO_NO_ACTIVO";
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            return "USUARIO_SIN_CORREO";
        }
        if (!notificacionRepository.esTipoEmailPrioridadAlta(notificacion.getTipo())) {
            return "TIPO_NO_ELEGIBLE";
        }
        return null;
    }

    public boolean esElegible(Notificacion notificacion, Usuario usuario) {
        return motivoNoElegible(notificacion, usuario) == null;
    }
}
