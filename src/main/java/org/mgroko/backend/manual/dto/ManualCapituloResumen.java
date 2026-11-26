package org.mgroko.backend.manual.dto;

/**
 * Resumen de un capítulo para el índice y la navegación anterior/siguiente.
 *
 * @param etiqueta texto alternativo para anexos; {@code null} en capítulos
 */
public record ManualCapituloResumen(
        String id,
        int numero,
        String etiqueta,
        String titulo) {
}
