package org.mgroko.backend.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(min = 2, max = 50) String nombre,
        @NotBlank @Size(min = 2, max = 50) String apellido,
        @NotBlank String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Email String correo,
        @NotBlank String password
) {}