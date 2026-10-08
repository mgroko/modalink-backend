package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigurarSchedulerRequest(
        @NotNull(message = "La hora es obligatoria.")
        @Min(value = 0, message = "La hora debe estar entre 0 y 23.")
        @Max(value = 23, message = "La hora debe estar entre 0 y 23.")
        Integer hora,

        @NotNull(message = "El minuto es obligatorio.")
        @Min(value = 0, message = "El minuto debe estar entre 0 y 59.")
        @Max(value = 59, message = "El minuto debe estar entre 0 y 59.")
        Integer minuto
) {
}
