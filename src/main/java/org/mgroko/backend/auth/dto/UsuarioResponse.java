package org.mgroko.backend.auth.dto;

public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String rolGlobal,
        String genero
) {
}