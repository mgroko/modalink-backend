package org.mgroko.backend.manual.dto;

/**
 * Sección del capítulo extraída de los títulos nivel 2 y 3 del Markdown.
 */
public record ManualSeccion(
        String id,
        String titulo,
        int nivel) {
}
