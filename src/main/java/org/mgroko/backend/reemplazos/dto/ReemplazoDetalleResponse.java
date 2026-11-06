package org.mgroko.backend.reemplazos.dto;

import java.util.List;

public record ReemplazoDetalleResponse(
        ReemplazoResumenResponse reemplazo,
        List<CandidatoReemplazoResponse> candidatos) {
}
