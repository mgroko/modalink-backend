package org.mgroko.backend.contratos.dto;

import java.util.List;

public record PrevisualizacionModificacionResponse(
        String texto,
        boolean puedeModificarse,
        List<ConflictoContratoResponse> conflictos
) {
}
