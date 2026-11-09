package org.mgroko.backend.contratos.dto;

import java.util.List;

public record PrevisualizacionContratoResponse(
        String texto,
        boolean puedeGenerarse,
        List<ConflictoContratoResponse> conflictos
) {
}
