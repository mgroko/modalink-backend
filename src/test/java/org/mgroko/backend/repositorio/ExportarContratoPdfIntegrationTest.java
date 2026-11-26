package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.servicio.ExportarContratoPdfService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ExportarContratoPdfIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private ExportarContratoPdfService exportarContratoPdfService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void directorExportaElContratoYRegistraElEventoDeAuditoria()
            throws Exception {

        Datos datos = crearProyectoConContrato();

        byte[] pdf = exportarContratoPdfService.exportar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1)
                    .replaceAll("\\s+", " ");
            assertThat(texto)
                    .contains("Cesión de derechos de imagen")
                    .contains("Cláusula UC-55")
                    .contains("Partes y firmas")
                    .contains("ModaLink · Contrato")
                    .contains("Impreso el");
        }

        Integer eventos = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM auditoria
                 WHERE tipo_evento = 'CONTRATO_EXPORTADO'
                   AND id_afectado = ?
                """, Integer.class, datos.idContrato());
        assertThat(eventos).isEqualTo(1);
    }

    @Test
    void unaParteFirmanteTambienPuedeExportar() {
        Datos datos = crearProyectoConContrato();

        byte[] pdf = exportarContratoPdfService.exportar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void unUsuarioAjenoNoPuedeExportar() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> exportarContratoPdfService.exportar(
                datos.ajeno().getIdUsuario(),
                datos.perfilAjeno().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato()))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }
}
