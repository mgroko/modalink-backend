package org.mgroko.backend.publicaciones.dto;

/**
 * Resumen de una publicación dentro de una colaboración automática.
 */
public record PublicacionColaboracionAutomaticaResponse(
        Long idPublicacion,
        String titulo,
        String portadaUrl,
        String estado,
        boolean eliminada
) {
}
