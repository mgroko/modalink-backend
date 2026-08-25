package org.mgroko.backend.admin.dto;

import java.time.LocalDate;

public record AdminUsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String correo,
        String estado,
        String rolGlobal,
        LocalDate fechaNacimiento
) {}