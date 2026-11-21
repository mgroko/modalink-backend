package org.mgroko.backend.auditoria.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenEntidadResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenTipoResponse;
import org.mgroko.backend.auditoria.pdf.AuditoriaPdfRenderer.DatosInforme;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class AuditoriaPdfRendererTest {

    private static final LocalDateTime GENERADO_EN =
            LocalDateTime.of(2026, 11, 17, 21, 11, 44);
    private static final LocalDateTime IMPRESO_EN =
            LocalDateTime.of(2026, 11, 18, 17, 6, 28);

    @Test
    void generaElInformeConEncabezadoIndicadoresResumenYPie() throws Exception {
        byte[] pdf = generar(informeDeEjemplo(1));

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);

            String texto = plano(
                    new PdfTextExtractor(reader).getTextFromPage(1));
            assertThat(texto)
                    .contains("Informe de auditoría")
                    .contains("Período")
                    .contains("11/11/2026")
                    .contains("17/11/2026")
                    .contains("ModaLink · Registro de eventos del sistema")
                    .contains("TIPOS DE EVENTO")
                    .contains("GENERADO POR")
                    .contains("Admin ModaLink")
                    .contains("Total de registros")
                    .contains("Envío de mensaje privado")
                    .contains("Resumen por tipo de evento")
                    .contains("Resumen por entidad afectada")
                    .contains("Eventos")
                    .contains("mensaje_proyecto / 91")
                    .contains("Impreso el 18/11/2026 17:06:28")
                    .contains("ModaLink · Informe de auditoría")
                    .contains("Hoja 1 / 1");
        }
    }

    @Test
    void numeraTodasLasHojasConElTotalDelInforme() throws Exception {
        byte[] pdf = generar(informeDeEjemplo(40));

        try (PdfReader reader = new PdfReader(pdf)) {
            int total = reader.getNumberOfPages();
            assertThat(total).isGreaterThan(1);

            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int hoja = 1; hoja <= total; hoja++) {
                String texto = plano(extractor.getTextFromPage(hoja));
                assertThat(texto)
                        .contains("Impreso el 18/11/2026 17:06:28")
                        .contains("Hoja " + hoja + " / " + total);
            }
        }
    }

    @Test
    void informaSinRegistrosCuandoNoHayEventos() throws Exception {
        DatosInforme datos = new DatosInforme(
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 17),
                "Todos",
                "Admin ModaLink",
                GENERADO_EN,
                IMPRESO_EN,
                new AuditoriaResumenResponse(
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 17),
                        List.of(),
                        0,
                        List.of(),
                        List.of()),
                List.of(),
                5);

        byte[] pdf = generar(datos);

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = plano(
                    new PdfTextExtractor(reader).getTextFromPage(1));
            assertThat(texto)
                    .contains("Sin registros para los filtros seleccionados.")
                    .contains("Hoja 1 / 1");
        }
    }

    private DatosInforme informeDeEjemplo(int cantidadEventos) {
        List<AuditoriaResumenTipoResponse> tipos = List.of(
                new AuditoriaResumenTipoResponse(
                        "MENSAJE_PRIVADO_ENVIADO",
                        "Envío de mensaje privado",
                        9),
                new AuditoriaResumenTipoResponse(
                        "MENSAJE_PROYECTO_ENVIADO",
                        "Envío de mensaje de proyecto",
                        4));

        List<AuditoriaEventoResponse> eventos = new ArrayList<>();
        for (int i = 0; i < cantidadEventos; i++) {
            eventos.add(evento(91L - i));
        }

        return new DatosInforme(
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 17),
                "MENSAJE_PROYECTO_ENVIADO, MENSAJE_PRIVADO_ENVIADO",
                "Admin ModaLink",
                GENERADO_EN,
                IMPRESO_EN,
                new AuditoriaResumenResponse(
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 17),
                        List.of("MENSAJE_PROYECTO_ENVIADO"),
                        eventos.isEmpty() ? 0 : 13,
                        tipos,
                        List.of(
                                new AuditoriaResumenEntidadResponse("mensaje", 9),
                                new AuditoriaResumenEntidadResponse(
                                        "mensaje_proyecto", 4))),
                eventos,
                5);
    }

    private AuditoriaEventoResponse evento(long id) {
        return new AuditoriaEventoResponse(
                10_000L + id,
                LocalDateTime.of(2026, 11, 17, 20, 9, 37),
                "MENSAJE_PROYECTO_ENVIADO",
                "Envío de mensaje de proyecto",
                "mensaje_proyecto",
                String.valueOf(id),
                id,
                30L,
                "Franco Perez",
                "app_usuario",
                "127.0.0.1/32",
                null,
                false,
                "Franco Perez",
                List.of(
                        new AuditoriaDetalleResponse("idProyecto", null, "22"),
                        new AuditoriaDetalleResponse("idPerfilEmisor", null, "30"),
                        new AuditoriaDetalleResponse(
                                "contenido",
                                null,
                                "Chicas me doy de baja, no voy a poder")));
    }

    private byte[] generar(DatosInforme datos) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        new AuditoriaPdfRenderer().render(datos, salida);
        byte[] pdf = salida.toByteArray();

        assertThat(hojasDeclaradas(pdf)).isNotEmpty();
        return pdf;
    }

    private List<String> hojasDeclaradas(byte[] pdf) throws Exception {
        List<String> hojas = new ArrayList<>();
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            Pattern patron = Pattern.compile("Hoja (\\d+) / (\\d+)");
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                Matcher matcher = patron.matcher(plano(extractor.getTextFromPage(i)));
                if (matcher.find()) {
                    hojas.add(matcher.group());
                }
            }
        }
        return hojas;
    }

    private String plano(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
