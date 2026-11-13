package org.mgroko.backend.admin.dto;

/**
 * Resultado de previsualizar el procesamiento histórico.
 */
public record PreviewProcesamientoHistoricoResponse(
        long publicaciones,
        long imagenes,
        long imagenesSinSha256,
        long imagenesSinPhash,
        long imagenesIlegibles,
        long paresCandidatosEstimados) {
}
