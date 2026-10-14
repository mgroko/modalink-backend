package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.RolProyectoUsuario;

public record ProyectoPropioResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        String estado,
        String privacidad,
        RolProyectoUsuario rolUsuario,
        ActividadResumenResponse proximaActividad
) {}
