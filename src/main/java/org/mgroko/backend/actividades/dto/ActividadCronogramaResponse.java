package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.OrigenInicio;

/**
 * Fila del cronograma (UC-33) para el Gantt y la tabla CPM detallada.
 *
 * <p>{@code inicioProyectado} y {@code finProyectado} son la proyección CPM.
 * {@code inicioPersistido} y {@code finPersistido} son las fechas realmente
 * guardadas en la actividad. En un proyecto confirmado el Gantt debe priorizar
 * las persistidas; en uno no confirmado, las proyectadas.</p>
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
        Boolean tentativo,
        LocalDateTime inicioPersistido,
        LocalDateTime finPersistido,
        OrigenInicio origenInicio) {

    public ActividadCronogramaResponse {
        predecesoras = predecesoras == null ? List.of() : List.copyOf(predecesoras);
    }
}
