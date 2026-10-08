package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK en postulacion_actividad, postulacion_gral,
 * invitacion_actividad e invitacion_gral en ModaLinkBD.sql:
 * CHECK (estado IN ('PENDIENTE', 'RECHAZADA', 'ACEPTADA'))
 */
public enum EstadoSolicitud {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA;

    public static final EstadoSolicitud Pendiente = PENDIENTE;
    public static final EstadoSolicitud Aceptada = ACEPTADA;
    public static final EstadoSolicitud Rechazada = RECHAZADA;
}
