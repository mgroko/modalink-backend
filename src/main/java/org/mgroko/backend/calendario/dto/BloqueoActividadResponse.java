package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bloqueo calculado derivado de una actividad de un proyecto activo.
 * No se persiste: se deriva del cronograma y ya incluye el margen por
 * actividad configurado en la agenda.
 *
 * <p>Incluye el proyecto de la actividad y los perfiles del usuario dueño del
 * calendario que están asignados a ella, para poder identificarlos en el
 * detalle del calendario.</p>
 */
public record BloqueoActividadResponse(
        Long idActividad,
        String nombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        Long idProyecto,
        String nombreProyecto,
        List<PerfilCalendarioResponse> perfiles
) {

    public BloqueoActividadResponse {
        perfiles = perfiles != null ? List.copyOf(perfiles) : List.of();
    }

    /**
     * Constructor de compatibilidad para consumidores que todavía no
     * necesitan el proyecto ni los perfiles.
     */
    public BloqueoActividadResponse(
            Long idActividad,
            String nombre,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin) {
        this(idActividad, nombre, fechaHoraInicio, fechaHoraFin, null, null, List.of());
    }
}