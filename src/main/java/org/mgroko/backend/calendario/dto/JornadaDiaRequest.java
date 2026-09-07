package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * Día laborable de una agenda con su horario (formato 24h).
 * {@code diaSemana} usa ISO 8601: 1 = Lunes ... 7 = Domingo.
 *
 * Jornada de corrido: solo {@code horarioInicioManiana} y
 * {@code horarioFinTarde} (el par del mediodía ausente).
 * Jornada partida: las cuatro horas, en orden estricto.
 */
public record JornadaDiaRequest(
        @NotNull Integer diaSemana,
        @NotNull LocalTime horarioInicioManiana,
        LocalTime horarioFinManiana,
        LocalTime horarioInicioTarde,
        @NotNull LocalTime horarioFinTarde
) {
}
