package org.mgroko.backend.manual.dto;

import java.util.List;

/**
 * Capítulo del manual listo para mostrar.
 *
 * @param html       contenido renderizado y escapado
 * @param secciones  títulos de nivel 2 y 3 para el índice interno
 * @param anterior   capítulo anterior visible para el rol, o {@code null}
 * @param siguiente  capítulo siguiente visible para el rol, o {@code null}
 */
public record ManualCapituloResponse(
        String id,
        int numero,
        String etiqueta,
        String titulo,
        String html,
        List<ManualSeccion> secciones,
        ManualCapituloResumen anterior,
        ManualCapituloResumen siguiente) {
}
