package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

public record ActividadResponse(
        Long idActividad,
        Long idProyecto,
        String nombreProyecto,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horarioPreferenciaInicio,
        String estado,
        String estadoCronograma,
        UbicacionResponse ubicacion,
        List<ActividadVinculadaResponse> predecesoras,
        List<RequerimientoActividadResponse> requerimientos,
        List<ProfesionalAsignadoResponse> profesionalesAsignados
) {

    public ActividadResponse(
            Long idActividad,
            Long idProyecto,
            String nombreProyecto,
            String nombre,
            String descripcion,
            Integer duracionMinutos,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin,
            String estado,
            String estadoCronograma,
            UbicacionResponse ubicacion,
            List<ActividadVinculadaResponse> predecesoras,
            List<RequerimientoActividadResponse> requerimientos,
            List<ProfesionalAsignadoResponse> profesionalesAsignados) {
        this(idActividad, idProyecto, nombreProyecto, nombre, descripcion,
                duracionMinutos, fechaHoraInicio, fechaHoraFin, null, estado,
                estadoCronograma, ubicacion, predecesoras, requerimientos,
                profesionalesAsignados);
    }
}
