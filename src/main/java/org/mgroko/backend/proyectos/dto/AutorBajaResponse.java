package org.mgroko.backend.proyectos.dto;

public record AutorBajaResponse(
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        String nombre,
        String apellido
) {
}
