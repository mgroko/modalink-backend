package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de reprogramar un proyecto confirmado.
 */
public record ReprogramacionProyectoResponse(
        Long idProyecto,
        String nombreProyecto,
        String motivo,
        LocalDate fechaEntrega,
        List<ActividadReprogramadaResponse> actividades,
        List<ConflictoFijoResponse> conflictosFijos) {

    public ReprogramacionProyectoResponse {
        actividades = actividades == null ? List.of() : List.copyOf(actividades);
        conflictosFijos = conflictosFijos == null ? List.of() : List.copyOf(conflictosFijos);
    }

    public boolean huboCambios() {
        return !actividades.isEmpty();
    }
}
