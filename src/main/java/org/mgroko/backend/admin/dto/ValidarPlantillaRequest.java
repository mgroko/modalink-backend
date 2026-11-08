package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarPlantillaRequest(
        @NotBlank(message = "La plantilla es obligatoria.")
        @Size(max = 100_000, message = "La plantilla no puede superar los 100.000 caracteres.")
        String plantilla
) {
}
