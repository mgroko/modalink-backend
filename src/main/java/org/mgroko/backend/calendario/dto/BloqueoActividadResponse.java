package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

/**
 * Bloqueo calculado derivado de una actividad de un proyecto activo.
 * No se persiste: se deriva del cronograma y ya incluye el margen por
 * actividad configurado en la agenda.
 */
public record BloqueoActividadResponse(
        Long idActividad,
        String nombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin
) {
}