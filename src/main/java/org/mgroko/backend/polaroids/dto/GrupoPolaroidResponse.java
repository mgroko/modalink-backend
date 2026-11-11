package org.mgroko.backend.polaroids.dto;

import java.util.List;

public record GrupoPolaroidResponse(
        Long idGrupoPolaroid,
        String codigo,
        String nombre,
        List<TipoPolaroidResponse> tipos
) {
}
