package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminTipoContratoEstadoRequest(
        @NotNull(message = "El estado activo es obligatorio.")
        Boolean activo
) {
}
