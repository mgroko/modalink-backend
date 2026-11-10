package org.mgroko.backend.mensajeria.dto;

import java.util.List;

/**
 * Evento interno que se publica después del commit para notificar por SSE un
 * mensaje nuevo del chat grupal.
 */
public record MensajeGrupalEvento(
        Long idProyecto,
        Long idPerfilEmisor,
        List<Long> idPerfilesDestinatarios,
        MensajeGrupalStreamResponse mensaje
) {
}
