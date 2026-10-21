package org.mgroko.backend.notificaciones.mapper;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;

public final class NotificacionMapper {

    private NotificacionMapper() {
    }

    public static NotificacionResponse toResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getIdNotificacion(),
                notificacion.getTipo(),
                notificacion.getMensaje(),
                notificacion.getEntidadTipo(),
                notificacion.getEntidadId(),
                notificacion.getLeida(),
                notificacion.getFechaLeida(),
                notificacion.getOrigen(),
                notificacion.getFechaGenerada(),
                notificacion.getPerfilContexto() != null
                        ? notificacion.getPerfilContexto().getIdPerfil()
                        : null);
    }
}
