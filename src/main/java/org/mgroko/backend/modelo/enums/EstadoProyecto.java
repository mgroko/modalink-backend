package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla proyecto en ModaLinkBD.sql:
 * CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'CONFIRMADO', 'FINALIZADO', 'CANCELADO'))
 */
public enum EstadoProyecto {
    BORRADOR,
    PUBLICADO,
    CONFIRMADO,
    FINALIZADO,
    CANCELADO;

    public static final EstadoProyecto Borrador = BORRADOR;
    public static final EstadoProyecto Publicado = PUBLICADO;
    public static final EstadoProyecto Confirmado = CONFIRMADO;
    public static final EstadoProyecto Finalizado = FINALIZADO;
    public static final EstadoProyecto Cancelado = CANCELADO;

    public String getNombre() {
        return switch (this) {
            case BORRADOR -> "Borrador";
            case PUBLICADO -> "Publicado";
            case CONFIRMADO -> "Confirmado";
            case FINALIZADO -> "Finalizado";
            case CANCELADO -> "Cancelado";
        };
    }
}
