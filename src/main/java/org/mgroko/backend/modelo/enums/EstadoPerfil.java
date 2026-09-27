package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla perfil en ModaLinkBD.sql:
 * CHECK (estado IN ('ACTIVO', 'DESHABILITADO', 'PENDIENTE_BAJA', 'BAJA'))
 */
public enum EstadoPerfil {
    ACTIVO,
    DESHABILITADO,
    PENDIENTE_BAJA,
    BAJA;

    public static final EstadoPerfil Activo = ACTIVO;
    public static final EstadoPerfil Deshabilitado = DESHABILITADO;
    public static final EstadoPerfil PendienteBaja = PENDIENTE_BAJA;
    public static final EstadoPerfil Baja = BAJA;

    public String getNombre() {
        return switch (this) {
            case ACTIVO -> "Activo";
            case DESHABILITADO -> "Deshabilitado";
            case PENDIENTE_BAJA -> "PendienteBaja";
            case BAJA -> "Baja";
        };
    }
}
