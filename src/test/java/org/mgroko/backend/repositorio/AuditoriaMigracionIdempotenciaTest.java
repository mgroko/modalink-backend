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

    private static final String MIGRACION =
            "migraciones-test/2026-11-07_uc67_fixes_export.sql";

    private static final String MIGRACION_FASE01 =
            "migraciones-test/2026-11-08_uc67_auditoria_fase0_1.sql";

    private static final String MIGRACION_FASE24 =
            "migraciones-test/2026-11-08_uc67_auditoria_fase2_4.sql";

    private static final String MIGRACION_OPERADOR_BUSQUEDA =
            "migraciones-test/2026-11-12_auditoria_operador_busqueda_mensajes.sql";

    @Autowired
    private DataSource dataSource;

    @Test
    void migracionSePuedeEjecutarDosVecesSinErrores() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                aplicarDosVeces(conexion, MIGRACION);

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

    @Test
    void migracionFase0y1SePuedeEjecutarDosVecesSinErrores() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                aplicarDosVeces(conexion, MIGRACION_FASE01);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM tipo_evento_auditoria
                        WHERE codigo IN (
                            'CONTRATO_CREADO',
                            'CONTRATO_FIRMADO',
                            'TERMINOS_ACEPTADOS',
                            'USUARIO_DATOS_MODIFICADOS',
                            'PERFIL_CREADO')
                        """)).isEqualTo(5);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM information_schema.routines
                        WHERE routine_name = 'fn_auditoria_registrar'
                        """)).isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_trigger
                        WHERE tgname = 'trg_auditar_cambio_perfil'
                        """)).isEqualTo(1);
            } finally {
                conexion.rollback();
            }
        }
    }

    @Test
    void migracionFase2a4SePuedeEjecutarDosVecesSinErrores() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                aplicarDosVeces(conexion, MIGRACION_FASE24);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM tipo_evento_auditoria
                        WHERE codigo IN (
                            'MIEMBRO_ALTA',
                            'PROYECTO_CREADO',
                            'ACTIVIDAD_CREADA',
                            'CATALOGO_ALTA',
                            'REEMPLAZO_INICIADO')
                        """)).isEqualTo(5);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_trigger
                        WHERE tgname = 'trg_auditar_cambio_miembro'
                        """)).isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_trigger
                        WHERE tgname = 'trg_auditar_catalogo_habilidad'
                        """)).isEqualTo(1);
            } finally {
                conexion.rollback();
            }
        }
    }

    @Test
    void migracionOperadorBusquedaMensajesSePuedeEjecutarDosVecesSinErrores()
            throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                aplicarDosVeces(conexion, MIGRACION_OPERADOR_BUSQUEDA);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM information_schema.columns
                        WHERE table_name = 'auditoria'
                          AND column_name = 'operador_nombre'
                        """)).isEqualTo(1);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_trigger
                        WHERE tgname IN (
                            'trg_auditar_mensaje_privado',
                            'trg_auditar_mensaje_proyecto',
                            'trg_auditoria_completar_operador')
                        """)).isEqualTo(3);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM tipo_evento_auditoria
                        WHERE codigo IN (
                            'MENSAJE_PRIVADO_ENVIADO',
                            'MENSAJE_PROYECTO_ENVIADO')
                        """)).isEqualTo(2);

                assertThat(contar(conexion, """
                        SELECT count(*) FROM pg_proc
                        WHERE proname = 'f_unaccent'
                        """)).isEqualTo(1);
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
