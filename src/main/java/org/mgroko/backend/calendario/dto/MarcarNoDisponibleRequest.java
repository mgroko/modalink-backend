package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para marcar un bloque de tiempo como "No disponible" (UC-18).
 * El motivo es obligatorio (bloqueo_agenda.motivo NOT NULL en BD).
 */
public record MarcarNoDisponibleRequest(
        @NotNull LocalDateTime fechaHoraInicio,
        @NotNull LocalDateTime fechaHoraFin,
        @NotBlank(message = "El motivo del bloqueo es obligatorio.")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivo
) {
}