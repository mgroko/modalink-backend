package org.mgroko.backend.proyectos.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mgroko.backend.proyectos.support.InformeProyectoFixture;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class InformeProyectoPdfRendererTest {

    @TempDir
    Path tempDir;

    @Test
    void generaPdfConPortadaEstadoYMetadatos() throws Exception {
        PdfImagenResolver resolver = new PdfImagenResolver(tempDir.toString(), 1200);
        InformeProyectoPdfRenderer renderer = new InformeProyectoPdfRenderer(resolver, 12);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        renderer.render(InformeProyectoFixture.minimo(), salida);

        byte[] pdf = salida.toByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(pdf.length).isGreaterThan(2_000);

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(4);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String primera = extractor.getTextFromPage(1);
            assertThat(primera)
                    .contains("Campaña Primavera")
                    .contains("Estado por área")
                    .contains("Riesgo para la fecha de entrega")
                    .contains("Requiere tu decisión")
                    .contains("Generado por Ana Dir");

            String cuarta = extractor.getTextFromPage(reader.getNumberOfPages());
            assertThat(cuarta)
                    .contains("Alertas priorizadas")
                    .contains("Historial del equipo")
                    .contains("Cómo leer este informe");
        }
    }
}
