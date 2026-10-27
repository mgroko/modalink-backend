package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;

/**
 * Resultado de disponibilidad de un miembro dentro de una evaluación común.
 */
public record DetalleMiembroDisponibilidad(
        Long idUsuario,
        LocalDateTime inicioEvaluado,
        LocalDateTime finEvaluado,
        List<Intervalo> huecos,
        boolean disponible,
        String motivo) {

    public DetalleMiembroDisponibilidad {
        huecos = huecos == null ? List.of() : List.copyOf(huecos);
    }
}
