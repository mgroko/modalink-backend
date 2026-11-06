package org.mgroko.backend.proyectos.dto;

/**
 * Miembro activo del proyecto que debe ser calificado por el director
 * durante la finalización (UC-37).
 */
public record MiembroCalificableResponse(
        Long idPerfil,
        String nombreArtistico,
        String nombreUsuario,
        String apellidoUsuario,
        String rol,
        boolean yaCalificado) {
}
