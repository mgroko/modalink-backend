package org.mgroko.backend.publicaciones.dto;

public record AutorPublicacionResponse(
        Long idPerfil,
        String nombreArtistico,
        String profesion,
        String fotoUrl,
        String origen
) {

    /**
     * Constructor de compatibilidad para autores y respuestas históricas.
     */
    public AutorPublicacionResponse(
            Long idPerfil,
            String nombreArtistico,
            String profesion,
            String fotoUrl) {
        this(idPerfil, nombreArtistico, profesion, fotoUrl, null);
    }
}
