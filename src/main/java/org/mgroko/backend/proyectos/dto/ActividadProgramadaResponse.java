package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Horario propuesto o confirmado para una actividad.
 */
public record ActividadProgramadaResponse(
        Long idActividad,
        String nombre,
        LocalDateTime inicio,
        LocalDateTime fin,
        boolean fija) {
}
