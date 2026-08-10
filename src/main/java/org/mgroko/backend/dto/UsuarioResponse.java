package org.mgroko.backend.dto;

public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String rolGlobal
) {
}