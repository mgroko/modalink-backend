package org.mgroko.backend.auditoria.dto;

import java.time.LocalDate;
import java.util.List;

public record AuditoriaResumenResponse(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        List<String> tiposAplicados,
        long totalRegistros,
        List<AuditoriaResumenTipoResponse> porTipoEvento,
        List<AuditoriaResumenEntidadResponse> porEntidad
) {
}
