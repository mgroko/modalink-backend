package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Miembro visible en la vista pública del proyecto, junto con la versión de
 * T&C que quedó aceptada al incorporarse.
 *
 * <p>No expone ids internos de usuario ni datos de contacto.</p>
 */
public record MiembroPublicoTycProyectoResponse(
        Long idPerfil,
        String nombreArtistico,
        String nombreProfesion,
        String rol,
        Long idTycAceptado,
        Integer numeroVersionAceptado,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {
}
