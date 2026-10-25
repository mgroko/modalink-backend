package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;
import org.mgroko.backend.modelo.enums.EstadoProyecto;

public record AdminProyectoList(
        Long idProyecto,
        String nombre,
        LocalDateTime fechaInicio,
        EstadoProyecto estado,
        String privacidad
) {}