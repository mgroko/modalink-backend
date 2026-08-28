package org.mgroko.backend.perfiles.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearPerfilRequest(
        @NotBlank @Size(min = 2, max = 50) String nombreArtistico,
        @NotNull Long idProfesion,
        @NotBlank @Size(max = 500) String biografia,
        List<@Valid CaracteristicaPerfilRequest> caracteristicas
) {
}