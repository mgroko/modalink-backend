package org.mgroko.backend.mensajeria.dto;

import jakarta.validation.constraints.NotNull;

public record CrearConversacionRequest(
        @NotNull Long idPerfilDestinatario
) {
}
