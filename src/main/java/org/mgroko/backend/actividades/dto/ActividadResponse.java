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
        String tentative,        
        Boolean critica,          
        Long holguraMakespan,    
        Long holguraEntrega,    
        RangoFactible rangoInicio, 
        RangoFactible rangoEjecucion, 
        UbicacionResponse ubicacion,
        List<ActividadVinculadaResponse> predecesoras,
        List<RequerimientoActividadResponse> requerimientos,
        List<ProfesionalAsignadoResponse> profesionalesAsignados,
        LocalDateTime inicioProyectado,
        LocalDateTime finProyectado,
        Boolean tentativo,
        Boolean conflicto,
        String motivoConflicto,
        Boolean parcial
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
            LocalTime horarioPreferenciaInicio,
            String estado,
            String estadoCronograma,
            String tentative,
            Boolean critica,
            Long holguraMakespan,
            Long holguraEntrega,
            RangoFactible rangoInicio,
            RangoFactible rangoEjecucion,
            UbicacionResponse ubicacion,
            List<ActividadVinculadaResponse> predecesoras,
            List<RequerimientoActividadResponse> requerimientos,
            List<ProfesionalAsignadoResponse> profesionalesAsignados) {
        this(
                idActividad,
                idProyecto,
                nombreProyecto,
                nombre,
                descripcion,
                duracionMinutos,
                fechaHoraInicio,
                fechaHoraFin,
                horarioPreferenciaInicio,
                estado,
                estadoCronograma,
                tentative,
                critica,
                holguraMakespan,
                holguraEntrega,
                rangoInicio,
                rangoEjecucion,
                ubicacion,
                predecesoras,
                requerimientos,
                profesionalesAsignados,
                null,
                null,
                tentative == null ? null : Boolean.valueOf(tentative),
                null,
                null,
                null);
    }
}