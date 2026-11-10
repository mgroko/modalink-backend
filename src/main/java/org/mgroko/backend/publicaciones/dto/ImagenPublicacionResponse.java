package org.mgroko.backend.publicaciones.dto;

public record ImagenPublicacionResponse(
        Long idImagen,
        String url,
        int orden
) {
}
