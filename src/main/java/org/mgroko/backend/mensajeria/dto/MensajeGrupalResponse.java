package org.mgroko.backend.mensajeria.dto;

import java.time.LocalDateTime;

/**
 * Mensaje del chat grupal devuelto por la API REST.
 */
public record MensajeGrupalResponse(
        Long idMensajeProyecto,
        Long idProyecto,
        String contenido,
        LocalDateTime fechaEnvio,
        Long idPerfilEmisor,
        PerfilMensajeResponse perfilEmisor,
        boolean esMio
) {
}
