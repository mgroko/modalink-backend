package org.mgroko.backend.perfiles.dto;

/**
 * Datos públicos del perfil que emitió una reseña.
 */
public record AutorResenaResponse(
        Long idPerfil,
        String nombreArtistico,
        String profesion,
        String fotoUrl) {
}
