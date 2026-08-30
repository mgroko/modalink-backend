package org.mgroko.backend.perfiles.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarPerfilRequest(
        @NotBlank @Size(min = 2, max = 50) String nombreArtistico,
        @NotBlank @Size(max = 500) String biografia,
        Long idImagen,
        List<@Valid CaracteristicaPerfilRequest> caracteristicas
) {
}