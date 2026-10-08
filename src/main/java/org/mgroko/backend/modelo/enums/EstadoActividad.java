package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla actividad en ModaLinkBD.sql:
 * CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'FINALIZADA', 'CANCELADA'))
 */
public enum EstadoActividad {
    PENDIENTE,
    EN_CURSO,
    FINALIZADA,
    CANCELADA;

    public static final EstadoActividad Pendiente = PENDIENTE;
    public static final EstadoActividad EnCurso = EN_CURSO;
    public static final EstadoActividad Finalizada = FINALIZADA;
    public static final EstadoActividad Cancelada = CANCELADA;
}
