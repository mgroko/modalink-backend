package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifica que la migración de {@code numero_version} en {@code perfil_tyc}
 * sea idempotente y deje los objetos esperados.
 */
@SpringBootTest
class PerfilTycNumeroVersionMigracionTest extends AbstractPostgresIntegrationTest {

    private static final String MIGRACION =
            "migraciones-test/2026-11-26_uc23_numero_version_tyc.sql";

    @Autowired
    private DataSource dataSource;

    @Test
    void migracionSePuedeEjecutarDosVecesSinErrores() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                aplicarDosVeces(conexion, MIGRACION);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM information_schema.columns
                        WHERE table_name = 'perfil_tyc'
                          AND column_name = 'numero_version'
                        """))
                        .isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_indexes
                        WHERE tablename = 'perfil_tyc'
                          AND indexname = 'UQ_perfil_tyc_numero_version'
                        """))
                        .isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_trigger
                        WHERE tgname = 'trg_asignar_numero_version_tyc'
                        """))
                        .isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_constraint
                        WHERE conname = 'ck_tyc_perfil_numero_version_positiva'
                        """))
                        .isEqualTo(1);
            } finally {
                conexion.rollback();
            }
        }
    }

    private void aplicarDosVeces(Connection conexion, String recurso) throws Exception {
        String script = leerMigracion(recurso)
                .replace("\uFEFF", "")
                .replaceAll("(?im)^\\s*BEGIN;\\s*$", "")
                .replaceAll("(?im)^\\s*COMMIT;\\s*$", "");

        List<String> sentencias = dividirSentencias(script);
        assertThat(sentencias)
                .as("El divisor no encontró sentencias ejecutables")
                .isNotEmpty();

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
    }

    private static String leerMigracion(String recurso) throws Exception {
        ClassPathResource migracion = new ClassPathResource(recurso);
        assertThat(migracion.exists())
                .as("No se encontró la migración en el classpath: %s", recurso)
                .isTrue();
        return migracion.getContentAsString(StandardCharsets.UTF_8);
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
