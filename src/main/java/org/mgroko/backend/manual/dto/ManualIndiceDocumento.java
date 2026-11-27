package org.mgroko.backend.manual.dto;

import java.util.List;

/**
 * Documento fuente del índice del manual. Se carga desde
 * {@code classpath:manual/indice.json}; los capítulos Markdown se empaquetan
 * desde {@code docs/manual} por Maven.
 */
public record ManualIndiceDocumento(
        String version,
        List<Capitulo> capitulos) {

    public ManualIndiceDocumento {
        capitulos = capitulos == null ? List.of() : List.copyOf(capitulos);
    }

    public record Capitulo(
            String id,
            int numero,
            String etiqueta,
            String titulo,
            String archivo,
            List<String> roles) {

        public Capitulo {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
