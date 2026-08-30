package org.mgroko.backend.usuario.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record DatosPersonalesRequest(
        @NotBlank @Size(min = 2, max = 50) String nombre,
        @NotBlank @Size(min = 2, max = 50) String apellido,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank String genero,
        // id de la localidad del catálogo de Georef; null (o vacío) deja sin ubicación
        String localidadId
) {
}
