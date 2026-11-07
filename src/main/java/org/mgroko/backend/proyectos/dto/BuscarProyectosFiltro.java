package org.mgroko.backend.proyectos.dto;

import org.mgroko.backend.modelo.enums.EstadoProyecto;

public record BuscarProyectosFiltro(
        String nombre,
        EstadoProyecto estado,
        String director,
        Long idProfesion,
        String provinciaId,
        String localidadId
) {

    /**
     * Constructor de compatibilidad con la firma anterior (UC-29).
     */
    public BuscarProyectosFiltro(String nombre, EstadoProyecto estado, String director) {
        this(nombre, estado, director, null, null, null);
    }
}
