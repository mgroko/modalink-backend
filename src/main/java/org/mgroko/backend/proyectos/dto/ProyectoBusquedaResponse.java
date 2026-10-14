package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;

public record ProyectoBusquedaResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        String estado,
        String privacidad,
        Boolean aceptaPostulacionGral,
        Long idDirector,
        String nombreDirector
) {}
