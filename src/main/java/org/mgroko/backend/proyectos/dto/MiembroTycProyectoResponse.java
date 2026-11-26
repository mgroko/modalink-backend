package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Términos y condiciones aceptados por un miembro del proyecto.
 *
 * <p>{@code idTycAceptado} es la versión que quedó registrada al incorporar
 * al miembro. Puede no coincidir con su versión vigente actual.</p>
 */
public record MiembroTycProyectoResponse(
        Long idMiembro,
        Long idPerfil,
        String nombreArtistico,
        String nombreProfesion,
        Long idTycAceptado,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {
}
