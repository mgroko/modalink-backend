package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Mensaje del chat grupal enviado por SSE. No incluye {@code esMio} porque el
 * mismo evento se envía al emisor y al resto de los participantes.
 */
public record MensajeGrupalStreamResponse(
        Long idMensajeProyecto,
        Long idProyecto,
        String contenido,
        LocalDateTime fechaEnvio,
        Long idPerfilEmisor,
        PerfilMensajeResponse perfilEmisor
) {
}
