package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body para modificar cada cuántas horas se ejecuta la limpieza de imágenes.
 */
public record ConfigurarSchedulerLimpiezaImagenesRequest(
        @NotNull(message = "El intervalo es obligatorio.")
        @Min(value = 1, message = "El intervalo debe estar entre 1 y 168 horas.")
        @Max(value = 168, message = "El intervalo debe estar entre 1 y 168 horas.")
        Integer intervaloHoras
) {
}
