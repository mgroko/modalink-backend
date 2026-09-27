package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de miembros_proyecto en ModaLinkBD.sql:
 * CHECK (estado_participacion IN ('ACTIVO', 'BAJA_VOLUNTARIA', 'ELIMINADO'))
 */
public enum EstadoParticipacion {
    ACTIVO,
    BAJA_VOLUNTARIA,
    ELIMINADO;

    public static final EstadoParticipacion Activo = ACTIVO;
    public static final EstadoParticipacion BajaVoluntaria = BAJA_VOLUNTARIA;
    public static final EstadoParticipacion Eliminado = ELIMINADO;
}
