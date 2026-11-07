package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ejecuta la migración de UC-67 dos veces dentro de una transacción que se
 * revierte al final.
 *
 * <p>Comprueba que la migración es idempotente (se puede aplicar sobre una base
 * que ya tiene los objetos) y que no deja estado parcial: todo se revierte para
 * no contaminar la base compartida de los tests.</p>
 *
 * <p>Se usa un divisor propio porque {@code ScriptUtils} de Spring corta el
 * script en los {@code ;} que aparecen dentro de los bloques {@code $$} de
 * PL/pgSQL.</p>
 */
@SpringBootTest
class AuditoriaMigracionIdempotenciaTest extends AbstractPostgresIntegrationTest {

    private static final Path MIGRACION = Path.of(
            "docs", "3_diseño", "migraciones", "2026-11-07_uc67_fixes_export.sql");

    @Autowired
    private DataSource dataSource;

    @Test
    void migracionSePuedeEjecutarDosVecesSinErrores() throws Exception {
        assertThat(Files.isRegularFile(MIGRACION))
                .as("No se encontró la migración en %s", MIGRACION.toAbsolutePath())
                .isTrue();

        String script = Files.readString(MIGRACION, StandardCharsets.UTF_8)
                // El archivo se guarda con BOM UTF-8; PostgreSQL no acepta ese
                // carácter al inicio de una sentencia.
                .replace("\uFEFF", "")
                // El script trae BEGIN/COMMIT; acá se ejecuta en la transacción
                // de la prueba y se revierte al final.
                .replaceAll("(?im)^\\s*BEGIN;\\s*$", "")
                .replaceAll("(?im)^\\s*COMMIT;\\s*$", "");

        List<String> sentencias = dividirSentencias(script);
        assertThat(sentencias)
                .as("El divisor no encontró sentencias ejecutables")
                .isNotEmpty();

        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                for (int repetida = 0; repetida < 2; repetida++) {
                    try (Statement statement = conexion.createStatement()) {
                        for (String sentencia : sentencias) {
                            try {
                                statement.execute(sentencia);
                            } catch (java.sql.SQLException ex) {
                                throw new IllegalStateException(
                                        "Fallo en la repetición " + (repetida + 1)
                                                + " con la sentencia: ["
                                                + sentencia.substring(0,
                                                        Math.min(300, sentencia.length()))
                                                + "]",
                                        ex);
                            }
                        }
                    }
                }

                assertThat(contar(conexion,
                        "SELECT count(*) FROM tipo_evento_auditoria WHERE codigo = 'OTRO'"))
                        .isEqualTo(1);
                assertThat(contar(conexion,
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_name = 'auditoria' "
                                + "AND column_name = 'tipo_evento_original'"))
                        .isEqualTo(1);
                assertThat(contar(conexion,
                        "SELECT count(*) FROM configuracion_sistema "
                                + "WHERE clave IN ('AUDITORIA_EXPORT_MAX_REGISTROS', "
                                + "'AUDITORIA_PAGINA_MAX', "
                                + "'AUDITORIA_PDF_MAX_CAMBIOS_POR_FILA')"))
                        .isEqualTo(3);
            } finally {
                conexion.rollback();
            }
        }
    }

    private long contar(Connection conexion, String sql) throws Exception {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertThat(resultado.next()).isTrue();
            return resultado.getLong(1);
        }
    }

    /**
     * Divide el script en sentencias respetando comillas simples, comillas
     * dobles, comentarios de línea/bloque y bloques {@code $$} de PL/pgSQL.
     */
    private static List<String> dividirSentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        boolean enComillaSimple = false;
        boolean enComillaDoble = false;
        boolean enBloqueDollar = false;
        boolean enComentarioLinea = false;
        boolean enComentarioBloque = false;
        char anterior = 0;

        for (int i = 0; i < script.length(); i++) {
            char actual_ = script.charAt(i);
            char siguiente = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            if (enComentarioLinea) {
                actual.append(actual_);
                if (actual_ == '\n') {
                    enComentarioLinea = false;
                }
                anterior = actual_;
                continue;
            }

            if (enComentarioBloque) {
                actual.append(actual_);
                if (anterior == '*' && actual_ == '/') {
                    enComentarioBloque = false;
                }
                anterior = actual_;
                continue;
            }

            if (enBloqueDollar) {
                actual.append(actual_);
                if (anterior == '$' && actual_ == '$') {
                    enBloqueDollar = false;
                }
                anterior = actual_;
                continue;
            }

            if (!enComillaSimple && !enComillaDoble) {
                if (actual_ == '-' && siguiente == '-') {
                    enComentarioLinea = true;
                    actual.append(actual_);
                    anterior = actual_;
                    continue;
                }
                if (actual_ == '/' && siguiente == '*') {
                    enComentarioBloque = true;
                    actual.append(actual_);
                    anterior = actual_;
                    continue;
                }
                if (actual_ == '$' && siguiente == '$') {
                    enBloqueDollar = true;
                    actual.append(actual_).append(siguiente);
                    i++;
                    anterior = '$';
                    continue;
                }
            }

            if (actual_ == '\'' && !enComillaDoble) {
                enComillaSimple = !enComillaSimple;
            } else if (actual_ == '"' && !enComillaSimple) {
                enComillaDoble = !enComillaDoble;
            }

            if (actual_ == ';' && !enComillaSimple && !enComillaDoble) {
                agregar(sentencias, actual.toString());
                actual.setLength(0);
            } else {
                actual.append(actual_);
            }
            anterior = actual_;
        }

        agregar(sentencias, actual.toString());
        return sentencias;
    }

    private static void agregar(List<String> sentencias, String texto) {
        String sinComentarios = texto
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)--[^\n]*", " ")
                .trim();
        if (!sinComentarios.isEmpty()) {
            sentencias.add(texto.trim());
        }
    }
}
