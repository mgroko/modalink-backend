package org.mgroko.backend.admin.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;

/**
 * Filtros opcionales de la bandeja de reportes de perfil.
 *
 * <p>Si {@code estado} es {@code null} se devuelven todos los estados. El
 * frontend puede enviar {@code estado=PENDIENTE} para la bandeja de revisión.</p>
 */
public record BuscarReportesPerfilFiltro(
        EstadoReporte estado,
        MotivoReporte motivo,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {}
