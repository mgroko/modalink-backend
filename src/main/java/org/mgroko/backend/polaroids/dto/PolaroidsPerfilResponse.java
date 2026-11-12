package org.mgroko.backend.polaroids.dto;

import java.util.List;

public record PolaroidsPerfilResponse(
        Long idPerfil,
        int maximo,
        int total,
        List<PolaroidResponse> polaroids
) {
}
