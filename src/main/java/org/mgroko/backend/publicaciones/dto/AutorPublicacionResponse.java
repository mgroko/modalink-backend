package org.mgroko.backend.publicaciones.dto;

public record AutorPublicacionResponse(
        Long idPerfil,
        String nombreArtistico,
        String profesion,
        String fotoUrl
) {
}
