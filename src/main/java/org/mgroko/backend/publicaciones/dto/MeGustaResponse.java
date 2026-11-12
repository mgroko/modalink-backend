package org.mgroko.backend.publicaciones.dto;

/**
 * UC-66: estado del Me gusta de la publicación para el perfil activo.
 */
public record MeGustaResponse(
        Long idPublicacion,
        boolean meGusta,
        int cantidadMeGusta
) {
}
