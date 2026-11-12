package org.mgroko.backend.publicaciones.dto;

/**
 * Opción del selector de proyecto para buscar publicaciones de un perfil.
 */
public record ProyectoPublicacionFiltroResponse(
        Long idProyecto,
        String nombre
) {
}
