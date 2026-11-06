package org.mgroko.backend.admin.dto;

import org.mgroko.backend.modelo.enums.EstadoReporte;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado de un reporte por parte del administrador.
 *
 * <p>Solo se admite pasar de {@code PENDIENTE} a {@code RESUELTO} o
 * {@code DESCARTADO}.</p>
 */
public record ActualizarEstadoReporteRequest(
        @NotNull(message = "El estado es obligatorio.")
        EstadoReporte estado
) {}
