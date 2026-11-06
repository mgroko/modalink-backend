package org.mgroko.backend.admin.dto;

import java.util.List;

/**
 * Resultado de detectar o reparar inconsistencias de cronograma.
 */
public record ReparacionCronogramaResponse(
        boolean dryRun,
        int totalDetectadas,
        int totalReparadas,
        int totalFallidas,
        List<InconsistenciaCronogramaResponse> inconsistencias,
        List<String> errores) {

    public ReparacionCronogramaResponse {
        inconsistencias = inconsistencias == null ? List.of() : List.copyOf(inconsistencias);
        errores = errores == null ? List.of() : List.copyOf(errores);
    }
}
