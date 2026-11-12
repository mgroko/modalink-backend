package org.mgroko.backend.publicaciones.dto;

public record ProyectoPublicacionResponse(
        Long idProyecto,
        String nombre,
        boolean puedeVerProyecto
) {
}
