package org.mgroko.backend.proyectos.dto;

import org.mgroko.backend.modelo.enums.EstadoProyecto;

public record BuscarProyectosFiltro(
        String nombre,
        EstadoProyecto estado,
        String director
) {}
