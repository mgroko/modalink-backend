package org.mgroko.backend.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que el usuario completa después de autenticarse con Google. El correo
 * y el "sub" viajan en el ticket firmado, no en este request.
 */
public record CompletarRegistroGoogleRequest(
        @NotBlank @Size(min = 2, max = 50) String nombre,
        @NotBlank @Size(min = 2, max = 50) String apellido,
        @NotBlank @Pattern(regexp = "^[0-9]{1,15}$", message = "El DNI debe contener únicamente dígitos numéricos.")
        String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Size(max = 50) String genero,
        String localidadId) {
}
