package org.mgroko.backend.actividades.dto;

import java.util.List;

/**
 * Respuesta de UC-33: resumen, filas del Gantt/tabla CPM y conflictos.
 */
public record CronogramaResponse(
        ResumenCronogramaResponse resumen,
        List<ActividadCronogramaResponse> actividades,
        List<ConflictoCpm> conflictos) {

    public CronogramaResponse {
        actividades = actividades == null ? List.of() : List.copyOf(actividades);
        conflictos = conflictos == null ? List.of() : List.copyOf(conflictos);
    }
}
