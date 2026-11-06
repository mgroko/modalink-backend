package org.mgroko.backend.reemplazos.enums;

/**
 * Estado agregado de un proceso de reemplazo.
 */
public enum EstadoReemplazo {
    PENDIENTE_CONFIRMACION_DIRECTOR,
    INVITACIONES_ENVIADAS,
    PENDIENTE_APROBACION,
    CUBIERTO,
    SIN_CANDIDATOS,
    CANCELADO,
    EXPIRADO;

    public boolean esFinal() {
        return this == CUBIERTO
                || this == CANCELADO
                || this == EXPIRADO;
    }
}
