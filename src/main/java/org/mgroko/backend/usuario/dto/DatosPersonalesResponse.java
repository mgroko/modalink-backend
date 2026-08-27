package org.mgroko.backend.usuario.dto;

import java.time.LocalDate;

public record DatosPersonalesResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String genero,
        String ubicacion
) {
}
