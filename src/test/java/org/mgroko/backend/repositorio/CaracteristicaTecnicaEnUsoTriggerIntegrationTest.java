package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regresión del trigger {@code trg_caracteristica_en_uso_solo_nombre}
 * contra PostgreSQL real (el esquema se carga con {@code ModaLinkBD.sql}).
 *
 * Regla: si la característica está en uso por perfiles
 * ({@code caracteristica_perfil}) o por requerimientos, sólo puede
 * modificarse el nombre; cualquier cambio de codigo, tipo_dato,
 * id_profesion o id_unidad debe rechazarse con SQLSTATE 23514.
 *
 * Cada método deja el UPDATE fallido como última operación de la
 * transacción: en Postgres un error deja la transacción abortada, así que
 * sólo puede haber un fallo por test (el rollback final la revierte).
 */
@SpringBootTest
@Transactional
class CaracteristicaTecnicaEnUsoTriggerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    /**
     * Crea un usuario y un perfil MODELO, y le asigna la característica
     * sembrada ALTURA (id 1, profesión MODELO). Requiere que la transacción
     * del test vea las filas, por eso se inserta en la misma transacción.
     */
    private Long asignarAlturaAPerfil() {
        Long idUsuario = jdbc.queryForObject("""
                INSERT INTO usuario (nombre, apellido, dni, fecha_nacimiento, correo,
                                     estado, proveedor_auth, id_genero, id_rol_global)
                VALUES ('Trigger', 'Tester', '99000111', '1990-01-01', 'trigger.en.uso@mail.com',
                        'ACTIVO', 'LOCAL', 1, 1)
                RETURNING id_usuario
                """, Long.class);

        Long idPerfil = jdbc.queryForObject("""
                INSERT INTO perfil (id_usuario, estado, nombre_artistico, biografia, id_profesion)
                VALUES (?, 'ACTIVO', 'Trigger Tester', 'Bio de prueba', 2)
                RETURNING id_perfil
                """, Long.class, idUsuario);

        jdbc.update("""
                INSERT INTO caracteristica_perfil (valor, id_perfil, id_caracteristica)
                VALUES ('175', ?, 1)
                """, idPerfil);

        return idPerfil;
    }

    private String mensajesDeCausa(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            sb.append(causa.getMessage()).append(' ');
        }
        return sb.toString();
    }

    @Test
    void enUso_soloCambiaNombre_sePermite() {
        asignarAlturaAPerfil();

        assertTrue(caracteristicaTecnicaRepository.existeEnUso(1L),
                "existeEnUso debe detectar la asignación en caracteristica_perfil");
        assertTrue(caracteristicaTecnicaRepository.findIdsEnUso().contains(1L),
                "findIdsEnUso debe incluir la característica asignada");

        jdbc.update("UPDATE caracteristica_tecnica SET nombre = 'Altura editada' WHERE id_caracteristica = 1");

        assertEquals("Altura editada", jdbc.queryForObject(
                "SELECT nombre FROM caracteristica_tecnica WHERE id_caracteristica = 1", String.class));
    }

    @Test
    void enUso_cambiaCodigo_bloqueado() {
        asignarAlturaAPerfil();

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE caracteristica_tecnica SET codigo = 'ALTURA_MOD' WHERE id_caracteristica = 1"));

        assertTrue(mensajesDeCausa(ex).contains("en uso"),
                "el trigger debe rechazar el cambio de código: " + mensajesDeCausa(ex));
    }

    @Test
    void enUso_cambiaTipoDato_bloqueado() {
        asignarAlturaAPerfil();

        // Se limpia id_unidad en la misma sentencia para que el CHECK
        // chk_unidad_solo_numerico no sea el motivo del rechazo.
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE caracteristica_tecnica SET tipo_dato = 'TEXTO', id_unidad = NULL "
                                + "WHERE id_caracteristica = 1"));

        assertTrue(mensajesDeCausa(ex).contains("en uso"),
                "el trigger debe rechazar el cambio de tipo_dato: " + mensajesDeCausa(ex));
    }

    @Test
    void enUso_cambiaProfesion_bloqueado() {
        asignarAlturaAPerfil();

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE caracteristica_tecnica SET id_profesion = 1 WHERE id_caracteristica = 1"));

        assertTrue(mensajesDeCausa(ex).contains("en uso"),
                "el trigger debe rechazar el cambio de profesión: " + mensajesDeCausa(ex));
    }

    @Test
    void enUso_cambiaUnidad_bloqueado() {
        asignarAlturaAPerfil();

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE caracteristica_tecnica SET id_unidad = NULL WHERE id_caracteristica = 1"));

        assertTrue(mensajesDeCausa(ex).contains("en uso"),
                "el trigger debe rechazar el cambio de unidad: " + mensajesDeCausa(ex));
    }

    @Test
    void sinUso_cambiaTipoDato_sePermite() {
        Long id = jdbc.queryForObject("""
                INSERT INTO caracteristica_tecnica (codigo, nombre, tipo_dato, id_profesion)
                VALUES ('TEST_SIN_USO', 'Sin uso', 'TEXTO', 2)
                RETURNING id_caracteristica
                """, Long.class);

        jdbc.update("UPDATE caracteristica_tecnica SET tipo_dato = 'NUMERICO' WHERE id_caracteristica = ?", id);

        assertEquals("NUMERICO", jdbc.queryForObject(
                "SELECT tipo_dato FROM caracteristica_tecnica WHERE id_caracteristica = ?", String.class, id));
    }
}
