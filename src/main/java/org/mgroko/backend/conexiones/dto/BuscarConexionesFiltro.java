package org.mgroko.backend.conexiones.dto;

/**
 * Filtros de UC-44 bis. Todos son opcionales y se aplican a la contraparte.
 */
public record BuscarConexionesFiltro(
        String texto,
        String nombre,
        String apellido,
        String nombreArtistico,
        Long idProfesion
) {
}
