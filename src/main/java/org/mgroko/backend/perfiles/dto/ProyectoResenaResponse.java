package org.mgroko.backend.perfiles.dto;

/**
 * Proyecto en el que se originó una reseña.
 */
public record ProyectoResenaResponse(
        Long idProyecto,
        String nombre) {
}
