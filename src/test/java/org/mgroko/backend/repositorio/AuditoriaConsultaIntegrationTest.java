package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.servicio.AuditoriaExportService;
import org.mgroko.backend.auditoria.servicio.AuditoriaResumenService;
import org.mgroko.backend.auditoria.servicio.ListarAuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el listado, el resumen y las exportaciones de UC-67 contra
 * PostgreSQL real.
 */
@SpringBootTest
@Transactional
class AuditoriaConsultaIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final LocalDate HOY = LocalDate.now();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ListarAuditoriaService listarAuditoriaService;

    @Autowired
    private AuditoriaResumenService auditoriaResumenService;

    @Autowired
    private AuditoriaExportService auditoriaExportService;

    @Test
    void listadoSinTiposIncluyeHistoricosYEventosConocidos() {
        insertarAuditoria(null, null);
        insertarAuditoria("USUARIO_ALTA", "USUARIO_ALTA");

        var pagina = listarAuditoriaService.listar(
                HOY, HOY, null, null, null, 0, 20);

        assertThat(pagina.contenido())
                .anyMatch(evento -> evento.legacy())
                .anyMatch(evento -> "USUARIO_ALTA".equals(evento.tipoEvento()));
    }

    @Test
    void listadoConTiposExcluyeHistoricos() {
        insertarAuditoria(null, null);
        insertarAuditoria("USUARIO_ALTA", "USUARIO_ALTA");

        var pagina = listarAuditoriaService.listar(
                HOY, HOY, List.of("USUARIO_ALTA"), null, null, 0, 20);

        assertThat(pagina.contenido())
                .isNotEmpty()
                .allMatch(evento -> "USUARIO_ALTA".equals(evento.tipoEvento()));
    }

    @Test
    void resumenIncluyeSinTipoCuandoNoHayFiltro() {
        insertarAuditoria(null, null);
        insertarAuditoria("USUARIO_ALTA", "USUARIO_ALTA");

        AuditoriaResumenResponse resumen =
                auditoriaResumenService.obtenerResumen(HOY, HOY, null, null, null);

        assertThat(resumen.totalRegistros()).isGreaterThanOrEqualTo(2);
        assertThat(resumen.porTipoEvento())
                .anyMatch(tipo -> "SIN_TIPO".equals(tipo.codigo()))
                .anyMatch(tipo -> "USUARIO_ALTA".equals(tipo.codigo()));
    }

    @Test
    void exportacionCsvIncluyeEncabezadoYOperadorHistorico() {
        insertarAuditoria(null, null);
        insertarAuditoria("USUARIO_ALTA", "USUARIO_ALTA");

        AuditoriaFiltroResuelto filtro =
                auditoriaExportService.validarExportacion(HOY, HOY, null, null, null);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        auditoriaExportService.exportarCsv(filtro, salida);

        String csv = salida.toString(StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("id_auditoria;fecha_hora;tipo_evento")
                .contains("USUARIO_ALTA")
                .contains("No disponible (histórico)");

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM auditoria
                WHERE tipo_evento = 'AUDITORIA_EXPORTADA'
                """, Long.class)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void exportacionPdfGeneraUnPdfValido() {
        insertarAuditoria("USUARIO_ALTA", "USUARIO_ALTA");

        AuditoriaFiltroResuelto filtro =
                auditoriaExportService.validarExportacion(HOY, HOY, null, null, null);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        auditoriaExportService.exportarPdf(filtro, 1L, salida);

        assertThat(salida.size()).isGreaterThan(500);
        assertThat(salida.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void filtraPorOperadorSnapshotSinAcentos() {
        long idLuna = insertarAuditoriaConOperador("USUARIO_ALTA", "Luna Pérez");
        insertarAuditoriaConOperador("USUARIO_ALTA", "Sol Gómez");

        var pagina = listarAuditoriaService.listar(
                HOY, HOY, null, "perez", null, 0, 20);

        assertThat(pagina.contenido())
                .extracting(AuditoriaEventoResponse::idAuditoria)
                .containsExactly(idLuna);
        assertThat(pagina.contenido().getFirst().operador()).isEqualTo("Luna Pérez");
    }

    @Test
    void filtraPorTextoEnDetalleSinAcentos() {
        long idCoincide = insertarAuditoriaConOperador("USUARIO_ALTA", "Luna Pérez");
        insertarDetalle(idCoincide, "descripcion", null, "El perfil es un tonto");

        long idOtro = insertarAuditoriaConOperador("USUARIO_ALTA", "Sol Gómez");
        insertarDetalle(idOtro, "descripcion", null, "Texto sin la palabra buscada");

        var pagina = listarAuditoriaService.listar(
                HOY, HOY, null, null, "tonto", 0, 20);

        assertThat(pagina.contenido())
                .extracting(AuditoriaEventoResponse::idAuditoria)
                .containsExactly(idCoincide);
    }

    @Test
    void filtraPorTextoEnMetadatosDeAuditoria() {
        long idCoincide = insertarAuditoriaConOperador(
                "MENSAJE_PRIVADO_ENVIADO", "Luna Pérez");
        insertarAuditoriaConOperador("USUARIO_ALTA", "Sol Gómez");

        var pagina = listarAuditoriaService.listar(
                HOY, HOY, null, null, "mensaje_privado", 0, 20);

        assertThat(pagina.contenido())
                .extracting(AuditoriaEventoResponse::idAuditoria)
                .contains(idCoincide);
    }

    @Test
    void resumenAplicaFiltrosDeOperadorYTexto() {
        long idCoincide = insertarAuditoriaConOperador("USUARIO_ALTA", "Luna Pérez");
        insertarDetalle(idCoincide, "descripcion", null, "El perfil es un tonto");

        long idOtro = insertarAuditoriaConOperador("USUARIO_ALTA", "Sol Gómez");
        insertarDetalle(idOtro, "descripcion", null, "Texto sin la palabra buscada");

        AuditoriaResumenResponse resumen = auditoriaResumenService.obtenerResumen(
                HOY, HOY, null, "luna", "tonto");

        assertThat(resumen.totalRegistros()).isEqualTo(1);
    }

    private long insertarAuditoriaConOperador(
            String tipoEvento,
            String operadorNombre) {

        return jdbcTemplate.queryForObject("""
                INSERT INTO auditoria (
                    usuario_bd, entidad_afectada, id_afectado, fecha_hora,
                    ip_usuario, id_tipo_auditoria, id_usuario, operador_nombre,
                    tipo_evento, tipo_evento_original)
                VALUES ('postgres', 'usuario', 1, CURRENT_TIMESTAMP,
                        '127.0.0.1',
                        (SELECT id_tipo_auditoria FROM tipo_accion_auditoria WHERE nombre = 'UPDATE'),
                        1, ?, ?, ?)
                RETURNING id_auditoria
                """, Long.class, operadorNombre, tipoEvento, tipoEvento);
    }

    private void insertarDetalle(
            long idAuditoria,
            String campo,
            String anterior,
            String nuevo) {

        jdbcTemplate.update("""
                INSERT INTO detalle_auditoria (
                    campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES (?, ?, ?, ?)
                """, campo, anterior, nuevo, idAuditoria);
    }

    private void insertarAuditoria(String tipoEvento, String tipoEventoOriginal) {
        jdbcTemplate.update("""
                INSERT INTO auditoria (
                    usuario_bd, entidad_afectada, id_afectado, fecha_hora,
                    ip_usuario, id_tipo_auditoria, id_usuario,
                    tipo_evento, tipo_evento_original)
                VALUES ('postgres', 'usuario', 1, CURRENT_TIMESTAMP,
                        '127.0.0.1',
                        (SELECT id_tipo_auditoria FROM tipo_accion_auditoria WHERE nombre = 'UPDATE'),
                        NULL, ?, ?)
                """, tipoEvento, tipoEventoOriginal);
    }
}
