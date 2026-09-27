package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla usuario en ModaLinkBD.sql:
 * CHECK (estado IN ('ACTIVO', 'DESHABILITADO', 'PENDIENTE_BAJA', 'BAJA'))
 */
public enum EstadoUsuario {
    ACTIVO,
    DESHABILITADO,
    PENDIENTE_BAJA,
    BAJA;

    public static final EstadoUsuario Activo = ACTIVO;
    public static final EstadoUsuario Deshabilitado = DESHABILITADO;
    public static final EstadoUsuario PendienteBaja = PENDIENTE_BAJA;
    public static final EstadoUsuario Baja = BAJA;

    public boolean permiteAcceso() {
        return this == ACTIVO || this == PENDIENTE_BAJA;
    }
}
