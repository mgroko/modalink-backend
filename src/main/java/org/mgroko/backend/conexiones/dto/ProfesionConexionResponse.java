package org.mgroko.backend.conexiones.dto;

/**
 * Conteo de conexiones por profesión de la contraparte.
 */
public record ProfesionConexionResponse(
        Long idProfesion,
        String nombre,
        long cantidad
) {
}
