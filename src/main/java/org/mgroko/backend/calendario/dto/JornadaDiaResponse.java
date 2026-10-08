package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;

/**
 * Día laborable de una agenda. En una jornada de corrido, el par del
 * mediodía ({@code horaFinManana} y {@code horaInicioTarde}) es
 * {@code null}.
 */
public record JornadaDiaResponse(
        Integer diaSemana,
        LocalTime horaInicioManana,
        LocalTime horaFinManana,
        LocalTime horaInicioTarde,
        LocalTime horaFinTarde
) {
}
