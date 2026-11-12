package org.mgroko.backend.admin.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

/**
 * Filtros opcionales de la bandeja de reportes de publicación.
 *
 * <p>Si {@code estado} es {@code null} se devuelven todos los estados. El
 * frontend puede enviar {@code estado=PENDIENTE} para la bandeja de revisión.</p>
 */
public record BuscarReportesPublicacionFiltro(
        EstadoReporte estado,
        MotivoReportePublicacion motivo,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {}
