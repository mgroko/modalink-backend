package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Evento efímero de "escribiendo" dentro de una conversación.
 *
 * <p>No se persiste: sólo se reenvía por SSE al perfil destinatario.</p>
 */
public record EscribiendoEvento(
        Long idConversacion,
        Long idPerfilEmisor,
        Long idPerfilDestinatario,
        boolean escribiendo,
        LocalDateTime fechaEvento
) {
}
