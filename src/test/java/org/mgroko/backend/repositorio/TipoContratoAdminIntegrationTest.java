package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminTipoContratoCreateRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.servicio.AdminTipoContratoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integracion de UC-76 contra PostgreSQL real: permisos, indices, auditoria
 * y ciclo de vida basico del catalogo.
 */
@SpringBootTest
@Transactional
class TipoContratoAdminIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AdminTipoContratoService service;

    @Autowired
    private TipoAcuerdoContratoRepository tipoAcuerdoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String nombreUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void crearTipo_guardaCodigoActivoYAudita() {
        String nombre = nombreUnico("Acuerdo de prueba");

        AdminTipoContratoResponse creado = service.crear(
                new AdminTipoContratoCreateRequest(
                        nombre,
                        "Descripcion de prueba",
                        "Contrato {{tipo}}\nProyecto: {{proyecto}}\nPartes: {{partes}}\n"
                                + "{{clausulas_adicionales}}",
                        true));

        assertThat(creado.idTipoAcuerdo()).isNotNull();
        assertThat(creado.codigo()).startsWith("ACUERDO_DE_PRUEBA");
        assertThat(creado.activo()).isTrue();
        assertThat(creado.puedeEditar()).isTrue();

        List<Map<String, Object>> auditorias = jdbcTemplate.queryForList("""
                SELECT tipo_evento, entidad_afectada, entidad_clave
                FROM auditoria
                WHERE entidad_afectada = 'tipo_acuerdo_contrato'
                  AND entidad_clave = ?
                ORDER BY id_auditoria DESC
                """, creado.codigo());

        assertThat(auditorias).isNotEmpty();
        assertThat(auditorias.get(0).get("tipo_evento"))
                .isEqualTo("TIPO_CONTRATO_CREADO");

        List<Map<String, Object>> detalles = jdbcTemplate.queryForList("""
                SELECT valor_nuevo
                FROM detalle_auditoria
                WHERE id_auditoria = (
                    SELECT id_auditoria FROM auditoria
                    WHERE entidad_afectada = 'tipo_acuerdo_contrato'
                      AND entidad_clave = ?
                    ORDER BY id_auditoria DESC LIMIT 1
                )
                  AND campo = 'plantilla'
                """, creado.codigo());

        assertThat(detalles).hasSize(1);
        assertThat(String.valueOf(detalles.get(0).get("valor_nuevo")))
                .contains("[omitido:");
    }

    @Test
    void desactivarYReactivar_actualizaElCatalogoYAuditaEstado() {
        String nombre = nombreUnico("Acuerdo desactivable");
        AdminTipoContratoResponse creado = service.crear(
                new AdminTipoContratoCreateRequest(
                        nombre, null, "Contrato {{proyecto}} {{partes}}", true));

        AdminTipoContratoResponse desactivado = service.cambiarEstado(
                creado.idTipoAcuerdo(),
                new org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest(false));

        assertThat(desactivado.activo()).isFalse();
        assertThat(tipoAcuerdoRepository.findByActivoTrueOrderByNombreAsc())
                .noneMatch(t -> t.getIdTipoAcuerdo().equals(creado.idTipoAcuerdo()));

        List<Map<String, Object>> auditorias = jdbcTemplate.queryForList("""
                SELECT tipo_evento
                FROM auditoria
                WHERE entidad_afectada = 'tipo_acuerdo_contrato'
                  AND entidad_clave = ?
                ORDER BY id_auditoria DESC
                """, creado.codigo());

        assertThat(auditorias.get(0).get("tipo_evento"))
                .isEqualTo("TIPO_CONTRATO_ESTADO_CAMBIADO");

        AdminTipoContratoResponse reactivado = service.cambiarEstado(
                creado.idTipoAcuerdo(),
                new org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest(true));

        assertThat(reactivado.activo()).isTrue();
    }

    @Test
    void eliminarSinContratos_borraFisicamente() {
        AdminTipoContratoResponse creado = service.crear(
                new AdminTipoContratoCreateRequest(
                        nombreUnico("Acuerdo eliminable"),
                        null,
                        "Contrato {{proyecto}} {{partes}}",
                        true));

        service.eliminar(creado.idTipoAcuerdo());

        assertThat(tipoAcuerdoRepository.findById(creado.idTipoAcuerdo())).isEmpty();
    }

    @Test
    void permisosDeTiposContrato_estanAsignadosAlAdministrador() {
        List<String> permisos = jdbcTemplate.queryForList("""
                SELECT pg.nombre
                FROM permiso_global pg
                JOIN rol_global_permiso rgp ON rgp.id_permiso_global = pg.id_permiso_global
                JOIN rol_global rg ON rg.id_rol_global = rgp.id_rol_global
                WHERE rg.nombre = 'Administrador'
                  AND pg.nombre IN (
                      'VER_TIPOS_CONTRATO',
                      'CREAR_TIPO_CONTRATO',
                      'MODIFICAR_TIPO_CONTRATO',
                      'ELIMINAR_TIPO_CONTRATO'
                  )
                ORDER BY pg.nombre
                """, String.class);

        assertThat(permisos).containsExactly(
                "CREAR_TIPO_CONTRATO",
                "ELIMINAR_TIPO_CONTRATO",
                "MODIFICAR_TIPO_CONTRATO",
                "VER_TIPOS_CONTRATO");
    }
}
