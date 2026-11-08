package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.DeshabilitarUsuarioRequest;
import org.mgroko.backend.admin.servicio.AdminUsuarioService;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica los triggers de auditoría del MVP contra PostgreSQL real.
 *
 * <p>Todos los métodos corren dentro de una transacción de test y hacen
 * rollback al finalizar, por lo que no contaminan la base compartida.</p>
 */
@SpringBootTest
@Transactional
class AuditoriaTriggerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminUsuarioService adminUsuarioService;

    @Autowired
    private org.mgroko.backend.auditoria.servicio.AuditoriaEventoService auditoriaEventoService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void altaDeUsuario_registraEventoYAutoAtribucion() {
        long idUsuario = insertarUsuario("Alta");

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento, tipo_evento_original,
                       entidad_afectada, id_afectado
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_ALTA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idUsuario);

        assertThat(evento.get("tipo_evento")).isEqualTo("USUARIO_ALTA");
        assertThat(evento.get("tipo_evento_original")).isEqualTo("USUARIO_ALTA");
        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idUsuario);
        assertThat(((Number) evento.get("id_afectado")).longValue()).isEqualTo(idUsuario);
    }

    @Test
    void codigoDesconocidoSeResuelveComoOtro() {
        String resuelto = jdbcTemplate.queryForObject(
                "SELECT fn_evento_auditoria('EVENTO_INEXISTENTE')", String.class);

        assertThat(resuelto).isEqualTo("OTRO");
    }

    @Test
    void deshabilitarUsuario_registraAdminComoOperadorYDetalleCompleto() {
        long idAdmin = insertarUsuario("Admin");
        long idObjetivo = insertarUsuario("Objetivo");

        var autenticacion = new UsernamePasswordAuthenticationToken(
                String.valueOf(idAdmin), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        try {
            adminUsuarioService.deshabilitar(
                    idObjetivo,
                    idAdmin,
                    new DeshabilitarUsuarioRequest("Incumplimiento", 7));
            entityManager.flush();
        } finally {
            SecurityContextHolder.clearContext();
        }

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_DESHABILITADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idObjetivo);

        long idAuditoria = ((Number) evento.get("id_auditoria")).longValue();
        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                ORDER BY campo
                """, idAuditoria);

        assertThat(detalles)
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("estado");
                    assertThat(fila.get("valor_anterior")).isEqualTo("ACTIVO");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("DESHABILITADO");
                })
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("motivo_deshabilitacion");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("Incumplimiento");
                })
                .anySatisfy(fila -> assertThat(fila.get("campo"))
                        .isEqualTo("fecha_hasta_deshabilitacion"));
    }

    @Test
    void bajaDeUsuario_registraOperador() {
        long idAdmin = insertarUsuario("AdminBaja");
        long idObjetivo = insertarUsuario("ObjetivoBaja");

        setOperador(idAdmin);
        jdbcTemplate.update("""
                UPDATE usuario SET estado = 'BAJA' WHERE id_usuario = ?
                """, idObjetivo);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_BAJA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idObjetivo);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void habilitarUsuario_registraOperador() {
        long idAdmin = insertarUsuario("AdminHabilitar");
        long idObjetivo = insertarUsuario("ObjetivoHabilitar");

        setOperador(idAdmin);
        jdbcTemplate.update("""
                UPDATE usuario SET estado = 'DESHABILITADO' WHERE id_usuario = ?
                """, idObjetivo);
        jdbcTemplate.update("""
                UPDATE usuario SET estado = 'ACTIVO' WHERE id_usuario = ?
                """, idObjetivo);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_HABILITADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idObjetivo);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void cambioDeRol_registraAdminComoOperador() {
        long idAdmin = insertarUsuario("AdminRol");
        long idObjetivo = insertarUsuario("ObjetivoRol");

        setOperador(idAdmin);
        jdbcTemplate.update("""
                UPDATE usuario
                SET id_rol_global = (SELECT id_rol_global FROM rol_global WHERE codigo = 'ADMINISTRADOR')
                WHERE id_usuario = ?
                """, idObjetivo);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_ROL_CAMBIADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idObjetivo);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                """, ((Number) evento.get("id_auditoria")).longValue());

        assertThat(detalles)
                .anySatisfy(fila -> assertThat(fila.get("campo")).isEqualTo("id_rol_global"));
    }

    @Test
    void cambioDeEstadoDeProyecto_registraOperadorYEstado() {
        long idAdmin = insertarUsuario("AdminProyecto");
        setOperador(idAdmin);

        long idProyecto = jdbcTemplate.queryForObject("""
                INSERT INTO proyecto (nombre, descripcion, estado, privacidad)
                VALUES (?, 'Descripcion de prueba', 'BORRADOR', 'PUBLICO')
                RETURNING id_proyecto
                """, Long.class, "Proyecto auditoria " + SECUENCIA.incrementAndGet());

        jdbcTemplate.update("""
                UPDATE proyecto SET estado = 'PUBLICADO' WHERE id_proyecto = ?
                """, idProyecto);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'PROYECTO_ESTADO_CAMBIADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idProyecto);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                """, ((Number) evento.get("id_auditoria")).longValue());

        assertThat(detalles)
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("estado");
                    assertThat(fila.get("valor_anterior")).isEqualTo("BORRADOR");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("PUBLICADO");
                });
    }

    @Test
    void postulacion_registraCambioDeEstadoYEliminacion() {
        long idAdmin = insertarUsuario("AdminPostulacion");
        long idCandidato = insertarUsuario("Candidato");
        long idPerfil = insertarPerfil(idCandidato, "Candidato Auditoria");
        long idProyecto = insertarProyecto("Proyecto postulacion");
        long idRequerimiento = insertarRequerimiento(idProyecto);

        long idPostulacion = jdbcTemplate.queryForObject("""
                INSERT INTO postulacion_gral (estado, id_perfil, id_requerimiento_gral)
                VALUES ('PENDIENTE', ?, ?)
                RETURNING id_postulacion_gral
                """, Long.class, idPerfil, idRequerimiento);

        setOperador(idAdmin);
        jdbcTemplate.update("""
                UPDATE postulacion_gral SET estado = 'RECHAZADA' WHERE id_postulacion_gral = ?
                """, idPostulacion);

        Map<String, Object> cambio = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'postulacion_gral'
                  AND id_afectado = ?
                  AND tipo_evento = 'POSTULACION_ESTADO_CAMBIADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idPostulacion);

        assertThat(((Number) cambio.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        jdbcTemplate.update("DELETE FROM postulacion_gral WHERE id_postulacion_gral = ?", idPostulacion);

        Map<String, Object> eliminacion = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'postulacion_gral'
                  AND id_afectado = ?
                  AND tipo_evento = 'POSTULACION_ELIMINADA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idPostulacion);

        assertThat(((Number) eliminacion.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void invitacion_registraCambioDeEstadoYEliminacion() {
        long idAdmin = insertarUsuario("AdminInvitacion");
        long idRemitente = insertarUsuario("Remitente");
        long idDestinatario = insertarUsuario("Destinatario");
        long idPerfilRemitente = insertarPerfil(idRemitente, "Remitente Auditoria");
        long idPerfilDestinatario = insertarPerfil(idDestinatario, "Destinatario Auditoria");
        long idProyecto = insertarProyecto("Proyecto invitacion");

        long idInvitacion = jdbcTemplate.queryForObject("""
                INSERT INTO invitacion_gral (
                    estado, id_perfil_destinatario, id_perfil_remitente, id_proyecto)
                VALUES ('PENDIENTE', ?, ?, ?)
                RETURNING id_invitacion_gral
                """, Long.class, idPerfilDestinatario, idPerfilRemitente, idProyecto);

        setOperador(idAdmin);
        jdbcTemplate.update("""
                UPDATE invitacion_gral SET estado = 'RECHAZADA' WHERE id_invitacion_gral = ?
                """, idInvitacion);

        Map<String, Object> cambio = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'invitacion_gral'
                  AND id_afectado = ?
                  AND tipo_evento = 'INVITACION_ESTADO_CAMBIADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idInvitacion);

        assertThat(((Number) cambio.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        jdbcTemplate.update("DELETE FROM invitacion_gral WHERE id_invitacion_gral = ?", idInvitacion);

        Map<String, Object> eliminacion = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'invitacion_gral'
                  AND id_afectado = ?
                  AND tipo_evento = 'INVITACION_ELIMINADA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idInvitacion);

        assertThat(((Number) eliminacion.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void configuracionDelSistema_registraOperadorYValores() {
        long idAdmin = insertarUsuario("AdminConfig");
        setOperador(idAdmin);

        jdbcTemplate.update("""
                UPDATE configuracion_sistema
                SET valor = '120'
                WHERE clave = 'AUDITORIA_RANGO_MAX_DIAS'
                """);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento, entidad_clave
                FROM auditoria
                WHERE entidad_afectada = 'configuracion_sistema'
                  AND entidad_clave = 'AUDITORIA_RANGO_MAX_DIAS'
                  AND tipo_evento = 'CONFIGURACION_ACTUALIZADA'
                  AND id_tipo_auditoria = (
                      SELECT id_tipo_auditoria FROM tipo_accion_auditoria WHERE nombre = 'UPDATE'
                  )
                ORDER BY id_auditoria DESC
                LIMIT 1
                """);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                """, ((Number) evento.get("id_auditoria")).longValue());

        assertThat(detalles)
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("valor");
                    assertThat(fila.get("valor_anterior")).isEqualTo("90");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("120");
                });
    }

    @Test
    void datosPersonales_registraValoresAnterioresYNuevos() {
        long idAdmin = insertarUsuario("AdminDatos");
        long idObjetivo = insertarUsuario("ObjetivoDatos");
        setOperador(idAdmin);

        int n = SECUENCIA.incrementAndGet();
        String nuevoCorreo = "datos" + n + "@test.com";
        String nuevoDni = String.format("%08d", (n + 10_000_000) % 100_000_000);

        jdbcTemplate.update("""
                UPDATE usuario
                   SET nombre = ?, apellido = ?, correo = ?, dni = ?
                 WHERE id_usuario = ?
                """, "NombreNuevo", "ApellidoNuevo", nuevoCorreo, nuevoDni, idObjetivo);

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_DATOS_MODIFICADOS'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idObjetivo);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                """, ((Number) evento.get("id_auditoria")).longValue());

        assertThat(detalles)
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("nombre");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("NombreNuevo");
                })
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("correo");
                    assertThat(fila.get("valor_nuevo")).isEqualTo(nuevoCorreo);
                })
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("dni");
                    assertThat(fila.get("valor_nuevo")).isEqualTo(nuevoDni);
                });
    }

    @Test
    void perfil_registraCreacionBajaYReactivacion() {
        long idAdmin = insertarUsuario("AdminPerfil");
        setOperador(idAdmin);

        long idPerfil = insertarPerfil(idAdmin, "PerfilAudit");

        Map<String, Object> creacion = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'perfil'
                  AND id_afectado = ?
                  AND tipo_evento = 'PERFIL_CREADO'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idPerfil);

        assertThat(((Number) creacion.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        jdbcTemplate.update("""
                UPDATE perfil
                   SET estado = 'PENDIENTE_BAJA',
                       fecha_solicitud_baja = CURRENT_TIMESTAMP
                 WHERE id_perfil = ?
                """, idPerfil);

        jdbcTemplate.update("""
                UPDATE perfil SET estado = 'BAJA' WHERE id_perfil = ?
                """, idPerfil);

        jdbcTemplate.update("""
                UPDATE perfil
                   SET estado = 'ACTIVO',
                       fecha_solicitud_baja = NULL
                 WHERE id_perfil = ?
                """, idPerfil);

        List<String> eventos = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'perfil'
                  AND id_afectado = ?
                ORDER BY id_auditoria
                """, String.class, idPerfil);

        assertThat(eventos)
                .contains("PERFIL_CREADO", "PERFIL_BAJA_SOLICITADA",
                        "PERFIL_ELIMINADO", "PERFIL_REACTIVADO");
    }

    @Test
    void terminosCondiciones_registraVersionYAceptacion() {
        long idAdmin = insertarUsuario("AdminTyc");
        setOperador(idAdmin);

        long idPerfil = insertarPerfil(idAdmin, "PerfilTyc");

        Long idTyc = jdbcTemplate.queryForObject("""
                INSERT INTO perfil_tyc (id_perfil, descripcion, fecha_inicio)
                VALUES (?, 'Terminos de prueba', CURRENT_TIMESTAMP)
                RETURNING id_tyc
                """, Long.class, idPerfil);

        Map<String, Object> version = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'perfil'
                  AND id_afectado = ?
                  AND tipo_evento = 'TERMINOS_VERSION_CREADA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idPerfil);

        assertThat(((Number) version.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        long idProyecto = insertarProyecto("ProyectoTyc");
        Long idMiembro = jdbcTemplate.queryForObject("""
                INSERT INTO miembros_proyecto (
                    estado_participacion, id_proyecto, id_perfil,
                    id_rol_proyecto, id_tyc_aceptado)
                VALUES ('ACTIVO', ?, ?,
                        (SELECT id_rol_proyecto FROM rol_proyecto WHERE codigo = 'MIEMBRO'),
                        ?)
                RETURNING id_miembro
                """, Long.class, idProyecto, idPerfil, idTyc);

        Map<String, Object> aceptacion = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'miembro_proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'TERMINOS_ACEPTADOS'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idMiembro);

        assertThat(((Number) aceptacion.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void eventoDeAplicacion_registraOperadorYDetalle() {
        long idAdmin = insertarUsuario("AdminEvento");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        String.valueOf(idAdmin), null, List.of()));
        try {
            auditoriaEventoService.registrar(
                    "usuario",
                    idAdmin,
                    "USUARIO_DATOS_MODIFICADOS",
                    "UPDATE",
                    List.of(new AuditoriaDetalleRequest(
                            "campoPrueba", "antes", "despues")));
        } finally {
            SecurityContextHolder.clearContext();
        }

        Map<String, Object> evento = jdbcTemplate.queryForMap("""
                SELECT id_auditoria, id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'usuario'
                  AND id_afectado = ?
                  AND tipo_evento = 'USUARIO_DATOS_MODIFICADOS'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idAdmin);

        assertThat(((Number) evento.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT campo, valor_anterior, valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = ?
                """, ((Number) evento.get("id_auditoria")).longValue());

        assertThat(detalles)
                .anySatisfy(fila -> {
                    assertThat(fila.get("campo")).isEqualTo("campoPrueba");
                    assertThat(fila.get("valor_anterior")).isEqualTo("antes");
                    assertThat(fila.get("valor_nuevo")).isEqualTo("despues");
                });
    }

    @Test
    void membresia_registraAltaYBajaVoluntaria() {
        long idAdmin = insertarUsuario("AdminMiembro");
        long idPerfil = insertarPerfil(idAdmin, "MiembroAudit");
        long idProyecto = insertarProyecto("ProyectoMiembro");
        setOperador(idAdmin);

        Long idMiembro = jdbcTemplate.queryForObject("""
                INSERT INTO miembros_proyecto (
                    estado_participacion, id_proyecto, id_perfil, id_rol_proyecto)
                VALUES ('ACTIVO', ?, ?,
                        (SELECT id_rol_proyecto FROM rol_proyecto WHERE codigo = 'MIEMBRO'))
                RETURNING id_miembro
                """, Long.class, idProyecto, idPerfil);

        Map<String, Object> alta = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'miembros_proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'MIEMBRO_ALTA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idMiembro);

        assertThat(((Number) alta.get("id_usuario")).longValue()).isEqualTo(idAdmin);

        jdbcTemplate.update("""
                UPDATE miembros_proyecto
                   SET estado_participacion = 'BAJA_VOLUNTARIA',
                       "motivoBaja" = 'Motivo de prueba',
                       fecha_baja = fecha_alta
                 WHERE id_miembro = ?
                """, idMiembro);

        Map<String, Object> baja = jdbcTemplate.queryForMap("""
                SELECT id_usuario, tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'miembros_proyecto'
                  AND id_afectado = ?
                  AND tipo_evento = 'MIEMBRO_BAJA_VOLUNTARIA'
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, idMiembro);

        assertThat(((Number) baja.get("id_usuario")).longValue()).isEqualTo(idAdmin);
    }

    @Test
    void proyectoYActividad_registranCreacionModificacionYReprogramacion() {
        long idAdmin = insertarUsuario("AdminOperativo");
        setOperador(idAdmin);

        long idProyecto = insertarProyecto("ProyectoAuditOperativo");

        jdbcTemplate.update("""
                UPDATE proyecto
                   SET descripcion = 'Descripcion modificada'
                 WHERE id_proyecto = ?
                """, idProyecto);

        jdbcTemplate.update("""
                UPDATE proyecto
                   SET fecha_inicio = CURRENT_TIMESTAMP + INTERVAL '1 day',
                       fecha_finalizacion = CURRENT_TIMESTAMP + INTERVAL '10 days'
                 WHERE id_proyecto = ?
                """, idProyecto);

        long idPlanificacion = jdbcTemplate.queryForObject("""
                INSERT INTO planificacion (id_proyecto)
                VALUES (?)
                RETURNING id_planificacion
                """, Long.class, idProyecto);

        Long idActividad = jdbcTemplate.queryForObject("""
                INSERT INTO actividad (
                    estado, nombre, duracion_minutos, id_planificacion)
                VALUES ('PENDIENTE', 'Actividad auditada', 60, ?)
                RETURNING id_actividad
                """, Long.class, idPlanificacion);

        jdbcTemplate.update("""
                UPDATE actividad SET estado = 'EN_CURSO' WHERE id_actividad = ?
                """, idActividad);

        List<String> eventosProyecto = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'proyecto'
                  AND id_afectado = ?
                ORDER BY id_auditoria
                """, String.class, idProyecto);

        assertThat(eventosProyecto)
                .contains("PROYECTO_CREADO", "PROYECTO_MODIFICADO",
                        "PROYECTO_REPROGRAMADO");

        List<String> eventosActividad = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'actividad'
                  AND id_afectado = ?
                ORDER BY id_auditoria
                """, String.class, idActividad);

        assertThat(eventosActividad)
                .contains("ACTIVIDAD_CREADA", "ACTIVIDAD_ESTADO_CAMBIADO");
    }

    @Test
    void catalogo_registraAltaModificacionYBaja() {
        long idAdmin = insertarUsuario("AdminCatalogo");
        setOperador(idAdmin);

        int n = SECUENCIA.incrementAndGet();
        String codigo = "HAB_AUDIT_" + n;

        Long idHabilidad = jdbcTemplate.queryForObject("""
                INSERT INTO habilidad (nombre, codigo, descripcion)
                VALUES (?, ?, 'Habilidad de prueba')
                RETURNING id_habilidad
                """, Long.class, "Habilidad auditada " + n, codigo);

        jdbcTemplate.update("""
                UPDATE habilidad SET nombre = ? WHERE id_habilidad = ?
                """, "Habilidad modificada " + n, idHabilidad);

        jdbcTemplate.update("DELETE FROM habilidad WHERE id_habilidad = ?", idHabilidad);

        List<String> eventos = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'habilidad'
                  AND id_afectado = ?
                ORDER BY id_auditoria
                """, String.class, idHabilidad);

        assertThat(eventos)
                .contains("CATALOGO_ALTA", "CATALOGO_MODIFICACION", "CATALOGO_BAJA");
    }

    @Test
    void habilidadDePerfil_registraAltaYBaja() {
        long idAdmin = insertarUsuario("AdminHabilidadPerfil");
        long idPerfil = insertarPerfil(idAdmin, "PerfilHabilidad");
        setOperador(idAdmin);

        int n = SECUENCIA.incrementAndGet();
        Long idHabilidad = jdbcTemplate.queryForObject("""
                INSERT INTO habilidad (nombre, codigo)
                VALUES (?, ?)
                RETURNING id_habilidad
                """, Long.class, "Habilidad perfil " + n, "HAB_PERFIL_" + n);

        jdbcTemplate.update("""
                INSERT INTO habilidad_perfil (id_habilidad, id_perfil)
                VALUES (?, ?)
                """, idHabilidad, idPerfil);

        jdbcTemplate.update("""
                DELETE FROM habilidad_perfil
                WHERE id_habilidad = ? AND id_perfil = ?
                """, idHabilidad, idPerfil);

        List<String> eventos = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'perfil'
                  AND id_afectado = ?
                ORDER BY id_auditoria
                """, String.class, idPerfil);

        assertThat(eventos)
                .contains("PERFIL_HABILIDAD_ALTA", "PERFIL_HABILIDAD_BAJA");
    }

    @Test
    void auditoria_esInmutableAnteUpdate() {
        long idUsuario = insertarUsuario("InmutableUpdate");
        Long idAuditoria = jdbcTemplate.queryForObject("""
                SELECT id_auditoria FROM auditoria
                WHERE entidad_afectada = 'usuario' AND id_afectado = ?
                ORDER BY id_auditoria DESC LIMIT 1
                """, Long.class, idUsuario);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE auditoria SET usuario_bd = 'hack' WHERE id_auditoria = ?", idAuditoria))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_esInmutableAnteDelete() {
        long idUsuario = insertarUsuario("InmutableDelete");
        Long idAuditoria = jdbcTemplate.queryForObject("""
                SELECT id_auditoria FROM auditoria
                WHERE entidad_afectada = 'usuario' AND id_afectado = ?
                ORDER BY id_auditoria DESC LIMIT 1
                """, Long.class, idUsuario);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM auditoria WHERE id_auditoria = ?", idAuditoria))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void detalleAuditoria_esInmutableAnteUpdate() {
        long idUsuario = insertarUsuario("InmutableDetalle");
        Long idDetalle = jdbcTemplate.queryForObject("""
                SELECT d.id_detalle_auditoria
                FROM detalle_auditoria d
                JOIN auditoria a ON a.id_auditoria = d.id_auditoria
                WHERE a.entidad_afectada = 'usuario' AND a.id_afectado = ?
                ORDER BY d.id_detalle_auditoria DESC
                LIMIT 1
                """, Long.class, idUsuario);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE detalle_auditoria SET valor_nuevo = 'hack' WHERE id_detalle_auditoria = ?",
                idDetalle))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_esInmutableAnteTruncate() {
        insertarUsuario("InmutableTruncate");

        assertThatThrownBy(() -> jdbcTemplate.execute(
                "TRUNCATE auditoria, detalle_auditoria"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void auditoria_truncatePermitidoConBypassDeMantenimiento() {
        insertarUsuario("BypassTruncate");

        // Bypass transaccional: el trigger no lanza la excepción y el
        // TRUNCATE se ejecuta dentro de la transacción de test (se revierte
        // al terminar el método, por lo que no contamina la base compartida).
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.auditoria_admin', 'true', true)", String.class);

        assertThatCode(() -> jdbcTemplate.execute(
                "TRUNCATE auditoria, detalle_auditoria"))
                .doesNotThrowAnyException();

        Long restantes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auditoria", Long.class);
        assertThat(restantes).isZero();
    }

    private void setOperador(long idUsuario) {
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.id_usuario', ?, true)",
                String.class,
                String.valueOf(idUsuario));
    }

    private long insertarUsuario(String etiqueta) {
        int n = SECUENCIA.incrementAndGet();
        return jdbcTemplate.queryForObject("""
                INSERT INTO usuario (
                    nombre, apellido, dni, fecha_nacimiento, correo, estado,
                    id_genero, id_rol_global)
                VALUES (?, ?, ?, DATE '1990-01-01', ?, 'ACTIVO',
                        (SELECT id_genero FROM genero ORDER BY id_genero LIMIT 1),
                        (SELECT id_rol_global FROM rol_global WHERE codigo = 'USUARIO'))
                RETURNING id_usuario
                """, Long.class,
                "Audit" + n,
                etiqueta,
                String.format("%08d", n),
                "audit" + n + "@test.com");
    }

    private long insertarPerfil(long idUsuario, String nombreArtistico) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO perfil (
                    id_usuario, estado, nombre_artistico, biografia, id_profesion)
                VALUES (?, 'ACTIVO', ?, 'Biografia de prueba',
                        (SELECT id_profesion FROM profesion ORDER BY id_profesion LIMIT 1))
                RETURNING id_perfil
                """, Long.class, idUsuario, nombreArtistico + " " + SECUENCIA.incrementAndGet());
    }

    private long insertarProyecto(String nombre) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO proyecto (nombre, descripcion, estado, privacidad)
                VALUES (?, 'Descripcion de prueba', 'BORRADOR', 'PUBLICO')
                RETURNING id_proyecto
                """, Long.class, nombre + " " + SECUENCIA.incrementAndGet());
    }

    private long insertarRequerimiento(long idProyecto) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO requerimiento_gral_proyecto (cantidad, id_proyecto, id_profesion)
                VALUES (1, ?,
                        (SELECT id_profesion FROM profesion ORDER BY id_profesion LIMIT 1))
                RETURNING id_requerimiento_gral
                """, Long.class, idProyecto);
    }
}
