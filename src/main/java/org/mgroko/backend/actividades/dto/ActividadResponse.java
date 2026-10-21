package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.List;

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
        String estado,
        UbicacionResponse ubicacion,
        List<ActividadVinculadaResponse> predecesoras,
        List<RequerimientoActividadResponse> requerimientos
) {
}
