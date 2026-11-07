package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos segun el CHECK de la tabla contrato en ModaLinkBD.sql:
 * CHECK (estado IN ('BORRADOR', 'PENDIENTE_FIRMA', 'ACTIVO',
 *                   'RECHAZADO_POR_PARTES', 'CANCELADO', 'ELIMINADO'))
 */
public enum EstadoContrato {
    BORRADOR,
    PENDIENTE_FIRMA,
    ACTIVO,
    RECHAZADO_POR_PARTES,
    CANCELADO,
    ELIMINADO;

    public String getNombre() {
        return switch (this) {
            case BORRADOR -> "Borrador";
            case PENDIENTE_FIRMA -> "Pendiente de firma";
            case ACTIVO -> "Activo";
            case RECHAZADO_POR_PARTES -> "Rechazado por las partes";
            case CANCELADO -> "Cancelado";
            case ELIMINADO -> "Eliminado";
        };
    }
}
