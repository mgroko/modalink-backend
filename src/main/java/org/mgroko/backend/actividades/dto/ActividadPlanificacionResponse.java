package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fila de la tabla de Planificación (UC-52).
 *
 * <p>Es una vista liviana de gestión: no incluye CPM ni proyecciones. Las
 * fechas son las persistidas; las proyectadas se consultan en el cronograma
 * (UC-33).</p>
 *
 * @param responsables perfiles activos asignados a la actividad; vacío si no
 *                      tiene asignaciones
 */
public record ActividadPlanificacionResponse(
        Long idActividad,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        String estado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        List<ActividadVinculadaResponse> predecesoras,
        List<ProfesionalAsignadoResponse> responsables) {

    public ActividadPlanificacionResponse {
        predecesoras = predecesoras == null ? List.of() : List.copyOf(predecesoras);
        responsables = responsables == null ? List.of() : List.copyOf(responsables);
    }
}
