package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * Día laborable de una agenda con su horario (formato 24h).
 * {@code diaSemana} usa ISO 8601: 1 = Lunes ... 7 = Domingo.
 */
public record JornadaDiaRequest(
        @NotNull Integer diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin
) {
}