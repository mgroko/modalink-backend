package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EliminarIntegranteRequest(
        @NotBlank(message = "El motivo de eliminación es obligatorio.")
        @Size(max = 100, message = "El motivo de eliminación no puede superar los 100 caracteres.")
        String motivo
) {
}
