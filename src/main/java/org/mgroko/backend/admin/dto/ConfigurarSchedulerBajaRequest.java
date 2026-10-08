package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigurarSchedulerBajaRequest(
        @NotNull(message = "La hora es obligatoria.")
        @Min(value = 0, message = "La hora debe estar entre 0 y 23.")
        @Max(value = 23, message = "La hora debe estar entre 0 y 23.")
        Integer hora,

        @NotNull(message = "El minuto es obligatorio.")
        @Min(value = 0, message = "El minuto debe estar entre 0 y 59.")
        @Max(value = 59, message = "El minuto debe estar entre 0 y 59.")
        Integer minuto,

        @NotNull(message = "Los días de plazo son obligatorios.")
        @Min(value = 1, message = "Los días de plazo deben estar entre 1 y 365.")
        @Max(value = 365, message = "Los días de plazo deben estar entre 1 y 365.")
        Integer diasBaja
) {
}
