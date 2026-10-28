package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fila del cronograma (UC-33) para el Gantt y la tabla CPM detallada.
 */
public record ActividadCronogramaResponse(
        Long idActividad,
        String nombre,
        String descripcion,
        String estado,
        Integer duracionMinutos,
        List<ActividadVinculadaResponse> predecesoras,
        LocalDateTime inicioProyectado,
        LocalDateTime finProyectado,
        LocalDateTime finConHolgura,
        Boolean critica,
        Boolean enRutaCritica,
        Long holguraMakespan,
        Long holguraEntrega,
        RangoFactible rangoInicio,
        RangoFactible rangoEjecucion,
        Boolean conflicto,
        String motivoConflicto,
        Boolean fijo,
        Boolean tentativo) {

    public ActividadCronogramaResponse {
        predecesoras = predecesoras == null ? List.of() : List.copyOf(predecesoras);
    }
}
