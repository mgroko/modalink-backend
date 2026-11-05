package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de la previsualización de UC-37.
 */
public record PrevisualizacionFinalizacionResponse(
        boolean puedeFinalizar,
        String estado,
        LocalDate fechaEntrega,
        LocalDate fechaActual,
        List<BloqueoFinalizacionResponse> bloqueos,
        List<MiembroCalificableResponse> miembrosACalificar,
        int calificacionesRequeridas,
        int calificacionesRealizadas,
        int calificacionesPendientes) {

    public PrevisualizacionFinalizacionResponse {
        bloqueos = bloqueos == null ? List.of() : List.copyOf(bloqueos);
        miembrosACalificar = miembrosACalificar == null
                ? List.of()
                : List.copyOf(miembrosACalificar);
    }
}
