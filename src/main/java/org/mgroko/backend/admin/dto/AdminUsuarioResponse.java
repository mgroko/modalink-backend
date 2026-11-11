package org.mgroko.backend.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String correo,
        String estado,
        String rolGlobal,
        LocalDate fechaNacimiento,
        String dni,
        LocalDateTime fechaSolicitudBaja,
        String motivoDeshabilitacion,
        LocalDateTime fechaHastaDeshabilitacion,
        GeneroResponse genero
) {}
