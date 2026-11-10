package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Mensaje enviado por SSE. No incluye {@code esMio} porque el mismo evento se
 * envía al emisor y al receptor.
 */
public record MensajeStreamResponse(
        Long idMensaje,
        Long idConversacion,
        String contenido,
        LocalDateTime fechaEnvio,
        Long idPerfilEmisor,
        PerfilMensajeResponse perfilEmisor,
        boolean leido,
        LocalDateTime fechaLectura
) {
}
