package org.mgroko.backend.proyectos.dto;

import java.util.List;

/**
 * Estado de las calificaciones de un participante en un proyecto finalizado.
 */
public record CalificacionesProyectoResponse(
        Long idProyecto,
        boolean calificacionesCompletas,
        int calificacionesRequeridas,
        int calificacionesRealizadas,
        int calificacionesPendientes,
        List<MiembroCalificableResponse> miembrosACalificar,
        List<CalificacionResponse> calificacionesEmitidas) {

    public CalificacionesProyectoResponse {
        miembrosACalificar = miembrosACalificar == null
                ? List.of()
                : List.copyOf(miembrosACalificar);
        calificacionesEmitidas = calificacionesEmitidas == null
                ? List.of()
                : List.copyOf(calificacionesEmitidas);
    }
}
