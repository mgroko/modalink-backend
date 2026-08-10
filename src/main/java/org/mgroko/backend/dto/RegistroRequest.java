package org.mgroko.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record RegistroRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotBlank String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Email String correo,
        @NotBlank String password
) {
}