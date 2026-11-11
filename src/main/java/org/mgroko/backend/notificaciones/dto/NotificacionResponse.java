package org.mgroko.backend.notificaciones.dto;

import java.time.LocalDateTime;

/**
 * Notificación expuesta al frontend.
 *
 * <p>{@code idProyecto} se persiste al generar la notificación cuando el
 * proyecto se conoce. Para notificaciones históricas se deriva en lectura de
 * {@code entidadTipo + entidadId}. Es {@code null} cuando el tipo de
 * notificación no tiene un proyecto navegable o la entidad referenciada ya no
 * existe y no se pudo capturar el proyecto al generarla.</p>
 *
 * <p>Los campos {@code idActividad}, {@code idHito} e {@code idInvitacion} se
 * derivan de la referencia polimórfica y son {@code null} cuando no
 * corresponden al tipo de entidad.</p>
 *
 * <p>{@code link} es una ruta relativa del frontend para abrir la acción
 * asociada a la notificación. Para {@code CALIFICACION_SOLICITADA} apunta a
 * {@code /proyectos/{idProyecto}/calificaciones}. Las notificaciones sociales
 * de conexiones apuntan a {@code /solicitudes-conexion} o
 * {@code /red-contactos} según el tipo. Las solicitudes de colaboración
 * apuntan a {@code /publicaciones/{idPublicacion}}. En el resto de los tipos
 * es {@code null}.</p>
 */
public record NotificacionResponse(
        Long idNotificacion,
        String tipo,
        String titulo,
        String mensaje,
        String entidadTipo,
        Long entidadId,
        Boolean leida,
        LocalDateTime fechaLeida,
        String origen,
        LocalDateTime fechaGenerada,
        Long idPerfilContexto,
        Long idProyecto,
        Long idActividad,
        Long idHito,
        Long idInvitacion,
        String link
) {
}
