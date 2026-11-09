package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Conversación visible para el perfil activo.
 */
public record ConversacionResponse(
        Long idConversacion,
        PerfilMensajeResponse perfil,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimoMensaje,
        String ultimoMensaje,
        boolean tieneNoLeidos,
        boolean puedeEnviar
) {
}
