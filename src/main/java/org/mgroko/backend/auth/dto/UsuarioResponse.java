package org.mgroko.backend.auth.dto;
import java.time.LocalDate;

public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String rolGlobal,
        String genero,
        LocalDate fechaNacimiento
) {
}