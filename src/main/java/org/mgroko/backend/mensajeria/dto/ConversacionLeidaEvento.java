package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Evento SSE de confirmación de lectura.
 */
public record ConversacionLeidaEvento(
        Long idConversacion,
        Long idPerfilLector,
        Long idPerfilOtro,
        Long idUltimoMensajeLeido,
        LocalDateTime fechaLectura
) {
}
