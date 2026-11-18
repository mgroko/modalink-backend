package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Error individual de un procesamiento histórico.
 */
public record ErrorProcesamientoHistoricoResponse(
        Long idError,
        Long idPublicacion,
        Long idImagen,
        String nombreArchivo,
        String mensaje,
        LocalDateTime fechaRegistro) {
}
