package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUnidadMedidaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 50) String simbolo,
        @NotBlank @Size(max = 50) String tipoDatoPermitido
) {
}
