package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.auditoria.servicio.ListarAuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de carga manual de UC-67.
 *
 * <p>No se ejecuta con {@code mvn test} porque el nombre termina en {@code IT}.
 * Ejecutar con:</p>
 *
 * <pre>mvn -Dtest=AuditoriaPerformanceIT test</pre>
 *
 * <p>Siembra 100.000 eventos y exige p95 &lt; 2 segundos para una consulta
 * de 30 días con 3 tipos.</p>
 */
@SpringBootTest
@Transactional
@Tag("performance")
class AuditoriaPerformanceIT extends AbstractPostgresIntegrationTest {

    private static final int REGISTROS = 100_000;
    private static final int MEDICIONES = 10;
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 30);
    private static final List<String> TIPOS = List.of(
            "USUARIO_ALTA", "USUARIO_BAJA", "PROYECTO_ESTADO_CAMBIADO");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ListarAuditoriaService listarAuditoriaService;

    @Test
    void consultaDeAuditoriaCon100kRegistros_cumpleP95MenorA2Segundos() {
        sembrarEventos();

        for (int i = 0; i < 3; i++) {
            listarAuditoriaService.listar(DESDE, HASTA, TIPOS, null, null, 0, 20);
        }

        List<Long> duracionesMs = new ArrayList<>();
        for (int i = 0; i < MEDICIONES; i++) {
            long inicio = System.nanoTime();
            var pagina = listarAuditoriaService.listar(
                    DESDE, HASTA, TIPOS, null, null, 0, 20);
            long fin = System.nanoTime();
            assertThat(pagina.contenido()).hasSize(20);
            duracionesMs.add(Duration.ofNanos(fin - inicio).toMillis());
        }

        duracionesMs.sort(Long::compareTo);
        long p95 = duracionesMs.get((int) Math.ceil(MEDICIONES * 0.95) - 1);

        System.out.printf("Auditoría 100k: p50=%d ms, p95=%d ms, muestras=%s%n",
                duracionesMs.get(MEDICIONES / 2), p95, duracionesMs);

        assertThat(p95).isLessThan(2_000L);
    }

    private void sembrarEventos() {
        jdbcTemplate.update("""
                INSERT INTO auditoria (
                    usuario_bd, entidad_afectada, id_afectado, fecha_hora,
                    ip_usuario, id_tipo_auditoria, id_usuario, tipo_evento)
                SELECT 'postgres',
                       'usuario',
                       g,
                       TIMESTAMP '2026-01-01 00:00:00'
                           + ((g % 180) * INTERVAL '1 day'),
                       '127.0.0.1',
                       2,
                       NULL,
                       'USUARIO_ALTA'
                FROM generate_series(1, ?) AS g
                """, REGISTROS);
    }
}
