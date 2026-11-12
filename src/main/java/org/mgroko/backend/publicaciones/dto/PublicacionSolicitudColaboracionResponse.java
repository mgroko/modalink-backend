package org.mgroko.backend.publicaciones.dto;

/**
 * Resumen de la publicación asociada a una solicitud de colaboración.
 */
public record PublicacionSolicitudColaboracionResponse(
        Long idPublicacion,
        String titulo,
        String portadaUrl,
        String estado
) {
}
