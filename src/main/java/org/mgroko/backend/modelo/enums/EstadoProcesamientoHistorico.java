package org.mgroko.backend.modelo.enums;

/**
 * Estados del procesamiento histórico de colaboraciones.
 */
public enum EstadoProcesamientoHistorico {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADO,
    COMPLETADO_CON_ERRORES,
    CANCELADO,
    FALLIDO,
    INTERRUMPIDO
}
