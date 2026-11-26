package org.mgroko.backend.manual.dto;

/**
 * Resultado de búsqueda dentro del manual.
 */
public record ManualResultado(
        String capituloId,
        String capituloTitulo,
        String seccionId,
        String seccion,
        String fragmento) {
}
