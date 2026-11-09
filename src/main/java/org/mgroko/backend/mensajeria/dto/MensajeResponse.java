package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Mensaje devuelto por la API REST.
 */
public record MensajeResponse(
        Long idMensaje,
        Long idConversacion,
        String contenido,
        LocalDateTime fechaEnvio,
        Long idPerfilEmisor,
        PerfilMensajeResponse perfilEmisor,
        boolean esMio,
        boolean leido,
        LocalDateTime fechaLectura
) {
}
