package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActividadBusquedaResponse(
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
        List<ActividadVinculadaResponse> predecesoras
) {}
