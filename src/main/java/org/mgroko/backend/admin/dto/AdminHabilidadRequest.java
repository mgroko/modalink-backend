package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminHabilidadRequest(
        @NotBlank @Size(max = 255) String nombre,
        @Size(max = 255) String descripcion
) {
}
