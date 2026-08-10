package org.mgroko.backend.dto;

public record AuthResponse(
        String token,
        UsuarioResponse usuario
) {
}