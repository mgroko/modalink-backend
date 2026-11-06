package org.mgroko.backend.modelo.enums;

/**
 * Entidad sobre la que se emitió un reporte.
 *
 * <p>El modelo es genérico para reutilizarlo en UC-69 (reportar publicación).
 * En esta iteración solo se persisten reportes de tipo {@link #PERFIL}.</p>
 */
public enum EntidadReporte {
    PERFIL,
    PUBLICACION
}
