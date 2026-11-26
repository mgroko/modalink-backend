package org.mgroko.backend.normas.dto;

import java.util.List;

/**
 * Sección visible de las normas de convivencia. El contenido se publica como
 * datos estructurados para que la página pública y el manual in-app puedan
 * renderizarlo sin depender de un parser de Markdown.
 */
public record NormasConvivenciaSeccion(
        String id,
        String titulo,
        List<String> parrafos,
        List<String> items) {

    public NormasConvivenciaSeccion {
        parrafos = parrafos == null ? List.of() : List.copyOf(parrafos);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
