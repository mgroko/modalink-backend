package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;

/**
 * Día laborable de una agenda. En una jornada de corrido, el par del
 * mediodía ({@code horarioFinManiana} y {@code horarioInicioTarde}) es
 * {@code null}.
 */
public record JornadaDiaResponse(
        Integer diaSemana,
        LocalTime horarioInicioManiana,
        LocalTime horarioFinManiana,
        LocalTime horarioInicioTarde,
        LocalTime horarioFinTarde
) {
}
