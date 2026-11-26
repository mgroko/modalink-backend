package org.mgroko.backend.normas.dto;

import java.util.List;

/**
 * Documento fuente de las normas de convivencia. Se carga desde el recurso
 * {@code classpath:normas-convivencia/normas-convivencia.json}. Mantener todas
 * las versiones en el mismo archivo permite servir el historial sin consultar
 * una base de datos.
 */
public record NormasConvivenciaDocumento(
        String titulo,
        List<Version> versiones) {

    public NormasConvivenciaDocumento {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
    }

    public record Version(
            String version,
            String vigenteDesde,
            String vigenteHasta,
            String cambios,
            String regla,
            List<NormasConvivenciaSeccion> secciones) {

        public Version {
            secciones = secciones == null ? List.of() : List.copyOf(secciones);
        }
    }
}
