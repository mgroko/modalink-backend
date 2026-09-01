package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;

public record JornadaDiaResponse(
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}