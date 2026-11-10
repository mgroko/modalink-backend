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

    /**
     * Indica si el proyecto admite modificación de su información (UC-36),
     * incluido el moodboard. Un proyecto finalizado o cancelado es inmutable.
     */
    public boolean permiteModificacion() {
        return this == BORRADOR || this == PUBLICADO || this == CONFIRMADO;
    }

    /**
     * Indica si el proyecto admite reprogramación de actividades.
     *
     * <p>Los proyectos publicados y confirmados tienen cronograma calculable.
     * Un borrador no se programa y un proyecto finalizado o cancelado no se
     * modifica.</p>
     */
    public boolean permiteReprogramacion() {
        return this == PUBLICADO || this == CONFIRMADO;
    }
}
