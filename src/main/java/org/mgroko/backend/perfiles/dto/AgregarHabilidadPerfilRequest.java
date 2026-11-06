package org.mgroko.backend.perfiles.dto;

import jakarta.validation.constraints.NotNull;

public record AgregarHabilidadPerfilRequest(
        @NotNull(message = "El id de la habilidad es obligatorio.") Long idHabilidad
) {
}
