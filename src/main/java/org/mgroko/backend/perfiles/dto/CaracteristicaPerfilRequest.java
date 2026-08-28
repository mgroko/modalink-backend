package org.mgroko.backend.perfiles.dto;

import jakarta.validation.constraints.NotNull;

public record CaracteristicaPerfilRequest(
        @NotNull Long idCaracteristica,
        String valor
) {
}