package org.mgroko.backend.notificaciones.dto;

import java.time.LocalDateTime;

/**
 * Notificación expuesta al frontend.
 *
 * <p>{@code idProyecto} se deriva en lectura de {@code entidadTipo +
 * entidadId}. Es {@code null} cuando el tipo de notificación no tiene un
 * proyecto navegable o la entidad referenciada ya no existe.</p>
 */
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
        Long idPerfilContexto,
        Long idProyecto
) {
}
