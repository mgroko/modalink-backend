package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ActividadBusquedaResponse(
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
        List<RequerimientoActividadResumenResponse> requerimientos,
        List<ActividadVinculadaResponse> predecesoras
) {

    public ActividadBusquedaResponse(
            Long idActividad,
            Long idProyecto,
            String nombreProyecto,
            String nombre,
            String descripcion,
            Integer duracionMinutos,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin,
            String estado,
            List<RequerimientoActividadResumenResponse> requerimientos,
            List<ActividadVinculadaResponse> predecesoras) {
        this(idActividad, idProyecto, nombreProyecto, nombre, descripcion,
                duracionMinutos, fechaHoraInicio, fechaHoraFin, null, estado,
                requerimientos, predecesoras);
    }
}
