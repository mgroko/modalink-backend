package org.mgroko.backend.perfiles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC-23: texto que el usuario guarda como términos y condiciones de su perfil.
 */
public record ActualizarTerminosCondicionesRequest(
        @NotBlank(message = "El texto de los términos y condiciones es obligatorio.")
        @Size(max = 5000, message = "El texto no puede superar los 5000 caracteres.")
        String descripcion
) {
}
