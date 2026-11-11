package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Salida de UC-61: la solicitud de colaboración creada.
 */
public record SolicitudColaboracionResponse(
        Long idSolicitudColaboracion,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        Long idPublicacion,
        String tituloPublicacion,
        AutorPublicacionResponse remitente,
        AutorPublicacionResponse destinatario
) {
}
