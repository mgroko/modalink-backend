package org.mgroko.backend.mensajeria.dto;

/**
 * Evento SSE de mensaje nuevo.
 */
public record MensajeEvento(
        Long idConversacion,
        Long idPerfilEmisor,
        Long idPerfilDestinatario,
        MensajeStreamResponse mensaje
) {
}
