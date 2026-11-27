package org.mgroko.backend.contratos.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.pdf.ContratoPdfRenderer.DatosContratoPdf;
import org.mgroko.backend.contratos.pdf.ContratoPdfRenderer.PartePdf;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class ContratoPdfRendererTest {

    private static final LocalDateTime CREADO_EN =
            LocalDateTime.of(2026, 11, 10, 9, 15, 0);
    private static final LocalDateTime EMITIDO_EN =
            LocalDateTime.of(2026, 11, 12, 14, 30, 0);
    private static final LocalDateTime EXPORTADO_EN =
            LocalDateTime.of(2026, 11, 18, 17, 6, 28);

    private final ContratoPdfRenderer renderer = new ContratoPdfRenderer();

    @Test
    void generaElContratoConEncabezadoMetadatosPartesYClausulas()
            throws Exception {

        byte[] pdf = generar(datos(2));

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            String texto = plano(new PdfTextExtractor(reader)
                    .getTextFromPage(1));

            assertThat(texto)
                    .contains("CONTRATO")
                    .contains("Cesión de derechos de imagen - Proyecto Luna")
                    .contains("EMISIÓN")
                    .contains("12/11/2026 14:30")
                    .contains("v2")
                    .contains("Activo")
                    .contains("ModaLink · Proyecto Luna · Cesión de derechos")
                    .contains("TIPO DE ACUERDO")
                    .contains("GENERADO")
                    .contains("18/11/2026 17:06:28")
                    .contains("GENERADO POR")
                    .contains("Ana Pérez")
                    .contains("Datos del contrato")
                    .contains("Partes y firmas")
                    .contains("Luna Gómez")
                    .contains("Firmado")
                    .contains("Tomás Díaz")
                    .contains("Pendiente")
                    .contains("CLÁUSULAS ADICIONALES")
                    .contains("1. La parte cedente autoriza el uso de su imagen.")
                    .contains("Impreso el 18/11/2026 17:06:28")
                    .contains("ModaLink · Contrato")
                    .contains("Hoja 1 /");
            assertThat(sinEspacios(texto)).contains("Hoja1/1");
        }
    }

    @Test
    void numeraTodasLasHojasConElTotalDelContrato() throws Exception {
        byte[] pdf = generar(datos(120));

        try (PdfReader reader = new PdfReader(pdf)) {
            int total = reader.getNumberOfPages();
            assertThat(total).isGreaterThan(1);

            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int hoja = 1; hoja <= total; hoja++) {
                String texto = plano(extractor.getTextFromPage(hoja));
                assertThat(texto)
                        .contains("Impreso el 18/11/2026 17:06:28")
                        .contains("ModaLink · Contrato")
                        .contains("Hoja " + hoja + " /");
                assertThat(sinEspacios(texto))
                        .contains("Hoja" + hoja + "/" + total);
            }
        }
    }

    @Test
    void informaSinTextoCuandoElContratoNoTieneClausulas() throws Exception {
        DatosContratoPdf datos = datos(0);
        DatosContratoPdf sinTexto = new DatosContratoPdf(
                datos.idContrato(),
                datos.titulo(),
                datos.proyecto(),
                datos.tipoAcuerdo(),
                datos.estado(),
                datos.version(),
                datos.fechaCreacion(),
                datos.fechaEmision(),
                datos.fechaModificacion(),
                datos.fechaBaja(),
                datos.fechaCancelacion(),
                datos.motivoCancelacion(),
                "",
                datos.emisorNombre(),
                datos.partes(),
                datos.firmados(),
                datos.totalPartes(),
                datos.exportadoPor(),
                datos.exportadoEn(),
                datos.impresoEn());

        byte[] pdf = generar(sinTexto);

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = plano(new PdfTextExtractor(reader)
                    .getTextFromPage(1));
            assertThat(texto).contains("Sin texto disponible.");
        }
    }

    @Test
    void muestraElMotivoDeRechazoDeUnaParte() throws Exception {
        byte[] pdf = generar(datos(2));

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = plano(new PdfTextExtractor(reader)
                    .getTextFromPage(1));
            assertThat(texto)
                    .contains("Motivo de rechazo")
                    .contains("No coincide el alcance del proyecto.");
        }
    }

    private byte[] generar(DatosContratoPdf datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        renderer.render(datos, salida);
        return salida.toByteArray();
    }

    private DatosContratoPdf datos(int lineas) {
        List<PartePdf> partes = List.of(
                new PartePdf(
                        "Luna Gómez",
                        "30111222",
                        "DIRECTOR",
                        "FIRMADO",
                        LocalDateTime.of(2026, 11, 12, 16, 0, 0),
                        null,
                        null),
                new PartePdf(
                        "Tomás Díaz",
                        "28999888",
                        "DESTINATARIO",
                        "PENDIENTE",
                        null,
                        null,
                        null),
                new PartePdf(
                        "Ana Pérez",
                        "27666555",
                        "DESTINATARIO",
                        "RECHAZADO",
                        null,
                        LocalDateTime.of(2026, 11, 13, 10, 30, 0),
                        "No coincide el alcance del proyecto."));

        return new DatosContratoPdf(
                77L,
                "Cesión de derechos de imagen - Proyecto Luna",
                "Proyecto Luna",
                "Cesión de derechos",
                "ACTIVO",
                2,
                CREADO_EN,
                EMITIDO_EN,
                LocalDateTime.of(2026, 11, 13, 9, 0, 0),
                null,
                null,
                null,
                texto(lineas),
                "Luna Gómez",
                partes,
                1,
                3,
                "Ana Pérez",
                EXPORTADO_EN,
                EXPORTADO_EN);
    }

    private String texto(int clausulas) {
        StringBuilder sb = new StringBuilder();
        sb.append("CONTRATO DE CESIÓN DE DERECHOS DE IMAGEN\n\n");
        sb.append("Proyecto: Proyecto Luna\n");
        sb.append("Director emisor: Luna Gómez (DNI 30111222)\n");
        sb.append("Partes:\n");
        sb.append("- Tomás Díaz (DNI 28999888)\n");
        sb.append("- Ana Pérez (DNI 27666555)\n\n");
        sb.append("Las partes acuerdan las condiciones del presente contrato.\n\n");
        sb.append("CLÁUSULAS ADICIONALES\n");
        if (clausulas > 0) {
            sb.append("1. La parte cedente autoriza el uso de su imagen.\n");
        }
        for (int i = 2; i <= clausulas; i++) {
            sb.append(i)
                    .append(". Cláusula adicional de prueba número ")
                    .append(i)
                    .append(".\n");
        }
        return sb.toString();
    }

    private String plano(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }

    private String sinEspacios(String texto) {
        return texto.replaceAll("\\s+", "");
    }
}
