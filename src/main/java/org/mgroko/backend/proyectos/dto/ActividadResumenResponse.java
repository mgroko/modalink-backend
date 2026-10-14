package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

public record ActividadResumenResponse(
        Long idActividad,
        String nombre,
        LocalDateTime fechaHoraInicio,
        Integer duracionMinutos
) {}
