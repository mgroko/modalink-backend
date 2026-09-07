package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para marcar un bloque de tiempo como "No disponible" (UC-18).
 * El motivo es opcional (vacaciones, compromiso personal, etc.).
 */
public record MarcarNoDisponibleRequest(
        @NotNull LocalDateTime fechaHoraInicio,
        @NotNull LocalDateTime fechaHoraFin,
        @Size(max = 200) String motivo
) {
}