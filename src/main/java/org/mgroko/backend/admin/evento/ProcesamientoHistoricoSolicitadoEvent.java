package org.mgroko.backend.admin.evento;

/**
 * Solicita ejecutar un procesamiento histórico después de que la transacción
 * que creó el job confirma.
 */
public record ProcesamientoHistoricoSolicitadoEvent(Long idProcesamiento) {
}
