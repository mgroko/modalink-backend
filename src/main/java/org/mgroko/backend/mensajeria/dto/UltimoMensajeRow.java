package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Proyección del último mensaje de una conversación.
 */
public record UltimoMensajeRow(
        Long idConversacion,
        Long idMensaje,
        String contenido,
        LocalDateTime fechaEnvio,
        Long idPerfilEmisor
) {
}
