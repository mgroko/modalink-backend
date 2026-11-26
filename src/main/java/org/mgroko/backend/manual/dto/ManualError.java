package org.mgroko.backend.manual.dto;

/**
 * Entrada del catálogo de errores frecuentes del manual.
 *
 * @param id            identificador estable dentro del catálogo
 * @param capituloId     capítulo del manual donde se explica
 * @param capituloTitulo título del capítulo
 * @param seccionId      ancla de la sección dentro del capítulo
 * @param seccion        título de la sección
 * @param mensaje        mensaje mostrado al usuario
 * @param causa          por qué ocurre
 * @param solucion       qué hacer
 */
public record ManualError(
        String id,
        String capituloId,
        String capituloTitulo,
        String seccionId,
        String seccion,
        String mensaje,
        String causa,
        String solucion) {
}
