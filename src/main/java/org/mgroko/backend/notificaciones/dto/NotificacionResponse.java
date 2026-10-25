package org.mgroko.backend.notificaciones.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long idNotificacion,
        String tipo,
        String mensaje,
        String entidadTipo,
        Long entidadId,
        Boolean leida,
        LocalDateTime fechaLeida,
        String origen,
        LocalDateTime fechaGenerada,
        Long idPerfilContexto
) {
}
