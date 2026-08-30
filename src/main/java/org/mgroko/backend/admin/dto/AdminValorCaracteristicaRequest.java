package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminValorCaracteristicaRequest(
        Long idValor,
        @NotBlank @Size(max = 50) String codigo,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex debe tener formato #RRGGBB")
        @Size(max = 7) String colorHex
) {
}