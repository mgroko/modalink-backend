package org.mgroko.backend.reemplazos.enums;

/**
 * Estado de un candidato dentro de un proceso de reemplazo.
 */
public enum EstadoCandidatoReemplazo {
    PROPUESTO,
    INVITADO,
    ACEPTADO_PENDIENTE_APROBACION,
    APROBADO,
    RECHAZADO,
    RECHAZADO_ADMIN,
    EXPIRADO,
    CANCELADO;

    public boolean estaPendienteDeRespuesta() {
        return this == INVITADO;
    }

    public boolean estaActivo() {
        return this == PROPUESTO
                || this == INVITADO
                || this == ACEPTADO_PENDIENTE_APROBACION;
    }
}
