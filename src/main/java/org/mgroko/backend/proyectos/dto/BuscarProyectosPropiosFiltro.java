package org.mgroko.backend.proyectos.dto;

import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;

public record BuscarProyectosPropiosFiltro(
        String nombre,
        EstadoProyecto estado,
        RolProyectoUsuario rol
) {}
