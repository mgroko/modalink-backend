package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de UC-22. Todos son opcionales y se combinan con AND.
 *
 * @param fechaDesde  primer día incluido del rango de publicación
 * @param fechaHasta  último día incluido del rango de publicación
 * @param idProyecto  proyecto asociado
 * @param titulo      texto parcial del título
 * @param descripcion texto parcial de la descripción
 */
public record BuscarPublicacionesFiltro(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaDesde,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaHasta,

        Long idProyecto,
        String titulo,
        String descripcion
) {

    public boolean estaVacio() {
        return fechaDesde == null
                && fechaHasta == null
                && idProyecto == null
                && (titulo == null || titulo.isBlank())
                && (descripcion == null || descripcion.isBlank());
    }
}
