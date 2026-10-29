package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePerfilService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.reportes.dto.CrearReportePerfilRequest;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.reportes.servicio.ReportarPerfilService;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class ReportePerfilIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ReportarPerfilService reportarPerfilService;
    @Autowired
    private AdminReportePerfilService adminReportePerfilService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void reportarPerfil_persisteReporteAuditoriaYNotificaAlAdmin() {
        Escenario escenario = transactionTemplate.execute(status -> crearEscenario(EstadoPerfil.ACTIVO));

        reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                new CrearReportePerfilRequest(MotivoReporte.SPAM, "  Spam insistente  "));

        Map<String, Object> reporte = jdbcTemplate.queryForMap("""
                SELECT id_reporte, entidad_tipo, entidad_id, id_usuario_autor,
                       id_perfil_autor, id_perfil_reportado, motivo, descripcion, estado
                FROM reporte
                WHERE id_perfil_autor = ?
                ORDER BY id_reporte DESC
                LIMIT 1
                """, escenario.idPerfilAutor());

        Long idReporte = ((Number) reporte.get("id_reporte")).longValue();
        assertThat(reporte.get("entidad_tipo")).isEqualTo("PERFIL");
        assertThat(((Number) reporte.get("entidad_id")).longValue())
                .isEqualTo(escenario.idPerfilReportado());
        assertThat(((Number) reporte.get("id_usuario_autor")).longValue())
                .isEqualTo(escenario.idUsuarioAutor());
        assertThat(((Number) reporte.get("id_perfil_reportado")).longValue())
                .isEqualTo(escenario.idPerfilReportado());
        assertThat(reporte.get("motivo")).isEqualTo("SPAM");
        assertThat(reporte.get("descripcion")).isEqualTo("Spam insistente");
        assertThat(reporte.get("estado")).isEqualTo("PENDIENTE");

        Long auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'reporte'
                  AND id_afectado = ?
                  AND id_usuario = ?
                """, Long.class, idReporte, escenario.idUsuarioAutor());
        assertThat(auditorias).isEqualTo(1L);

        Long detalles = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM detalle_auditoria d
                JOIN auditoria a ON a.id_auditoria = d.id_auditoria
                WHERE a.entidad_afectada = 'reporte'
                  AND a.id_afectado = ?
                """, Long.class, idReporte);
        assertThat(detalles).isGreaterThanOrEqualTo(4L);

        var notificaciones = notificacionRepository
                .findByUsuarioDestino_IdUsuario(escenario.idUsuarioAdmin(), PageRequest.of(0, 20));

        assertThat(notificaciones.getContent())
                .filteredOn(notificacion -> NotificacionService.TIPO_PERFIL_REPORTADO
                        .equals(notificacion.getTipo()))
                .singleElement()
                .satisfies(notificacion -> {
                    assertThat(notificacion.getEntidadTipo()).isEqualTo("REPORTE_PERFIL");
                    assertThat(notificacion.getEntidadId()).isEqualTo(idReporte);
                    assertThat(notificacion.getIdProyecto()).isNull();
                    assertThat(notificacion.getPerfilContexto()).isNull();
                    assertThat(notificacion.getMensaje())
                            .contains("Spam o publicidad engañosa")
                            .contains("Spam insistente")
                            .contains("Autor UC15");
                });
    }

    @Test
    void reportarPerfil_duplicadoPendiente_lanzaConflicto() {
        Escenario escenario = transactionTemplate.execute(status -> crearEscenario(EstadoPerfil.ACTIVO));

        CrearReportePerfilRequest request =
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null);

        reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                request);

        assertThatThrownBy(() -> reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                request))
                .isInstanceOf(ReportePendienteExistenteException.class);

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reporte WHERE id_perfil_autor = ?
                """, Long.class, escenario.idPerfilAutor());
        assertThat(total).isEqualTo(1L);
    }

    @Test
    void reportarPerfil_targetNoActivo_lanzaNoEncontrado() {
        Escenario escenario = transactionTemplate.execute(
                status -> crearEscenario(EstadoPerfil.PENDIENTE_BAJA));

        assertThatThrownBy(() -> reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException.class);
    }

    @Test
    void buscar_admin_devuelveReporteConFiltros() {
        Escenario escenario = transactionTemplate.execute(status -> crearEscenario(EstadoPerfil.ACTIVO));

        reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                new CrearReportePerfilRequest(MotivoReporte.SPAM, "Spam"));

        PaginaResponse<AdminReportePerfilResumenResponse> pagina =
                adminReportePerfilService.buscar(
                        new BuscarReportesPerfilFiltro(
                                EstadoReporte.PENDIENTE, MotivoReporte.SPAM, null, null),
                        0, 20, false);

        assertThat(pagina.contenido())
                .filteredOn(fila -> fila.idPerfilAutor().equals(escenario.idPerfilAutor()))
                .singleElement()
                .satisfies(fila -> {
                    assertThat(fila.idPerfilReportado()).isEqualTo(escenario.idPerfilReportado());
                    assertThat(fila.motivo()).isEqualTo(MotivoReporte.SPAM);
                    assertThat(fila.estado()).isEqualTo(EstadoReporte.PENDIENTE);
                    assertThat(fila.nombrePerfilReportado()).startsWith("Reportado UC15");
                });
    }

    @Test
    void actualizarEstado_resuelto_auditaAdminYPermiteNuevoReporte() {        Escenario escenario = transactionTemplate.execute(status -> crearEscenario(EstadoPerfil.ACTIVO));

        reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null));

        Long idReporte = jdbcTemplate.queryForObject("""
                SELECT id_reporte FROM reporte
                WHERE id_perfil_autor = ? ORDER BY id_reporte DESC LIMIT 1
                """, Long.class, escenario.idPerfilAutor());

        AdminReportePerfilDetalleResponse detalle = adminReportePerfilService.actualizarEstado(
                idReporte,
                escenario.idUsuarioAdmin(),
                new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO));

        assertThat(detalle.estado()).isEqualTo(EstadoReporte.RESUELTO);
        assertThat(detalle.idAdminRevisor()).isEqualTo(escenario.idUsuarioAdmin());
        assertThat(detalle.fechaResolucion()).isNotNull();

        Map<String, Object> reporte = jdbcTemplate.queryForMap("""
                SELECT estado, id_admin_revisor, fecha_resolucion
                FROM reporte WHERE id_reporte = ?
                """, idReporte);
        assertThat(reporte.get("estado")).isEqualTo("RESUELTO");
        assertThat(((Number) reporte.get("id_admin_revisor")).longValue())
                .isEqualTo(escenario.idUsuarioAdmin());
        assertThat(reporte.get("fecha_resolucion")).isNotNull();

        Long auditoriasUpdate = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'reporte'
                  AND id_afectado = ?
                  AND id_usuario = ?
                  AND id_tipo_auditoria = (
                      SELECT id_tipo_auditoria FROM tipo_accion_auditoria WHERE nombre = 'UPDATE'
                  )
                """, Long.class, idReporte, escenario.idUsuarioAdmin());
        assertThat(auditoriasUpdate).isEqualTo(1L);

        Long detalleEstado = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM detalle_auditoria d
                JOIN auditoria a ON a.id_auditoria = d.id_auditoria
                WHERE a.entidad_afectada = 'reporte'
                  AND a.id_afectado = ?
                  AND d.campo = 'estado'
                  AND d.valor_anterior = 'PENDIENTE'
                  AND d.valor_nuevo = 'RESUELTO'
                """, Long.class, idReporte);
        assertThat(detalleEstado).isEqualTo(1L);

        reportarPerfilService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPerfilReportado(),
                new CrearReportePerfilRequest(MotivoReporte.CONTENIDO_OFENSIVO, null));

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reporte WHERE id_perfil_autor = ?
                """, Long.class, escenario.idPerfilAutor());
        assertThat(total).isEqualTo(2L);
    }

    private Escenario crearEscenario(EstadoPerfil estadoTarget) {
        String sufijo = String.valueOf(System.nanoTime());
        RolGlobal rolUsuario = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        RolGlobal rolAdmin = rolGlobalRepository.findByNombre("Administrador").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Profesion profesion = em.find(Profesion.class, 2L);

        Usuario autor = usuario("Autor", "UC15", sufijo + "1", "uc15.autor." + sufijo, rolUsuario, genero);
        Usuario usuarioReportado = usuario(
                "Reportado", "UC15", sufijo + "2", "uc15.reportado." + sufijo, rolUsuario, genero);
        Usuario admin = usuario(
                "Admin", "UC15", sufijo + "3", "uc15.admin." + sufijo, rolAdmin, genero);

        Perfil perfilAutor = perfil(
                "Autor UC15 " + sufijo, autor, profesion, EstadoPerfil.ACTIVO);
        Perfil perfilReportado = perfil(
                "Reportado UC15 " + sufijo, usuarioReportado, profesion, estadoTarget);

        return new Escenario(
                autor.getIdUsuario(),
                perfilAutor.getIdPerfil(),
                perfilReportado.getIdPerfil(),
                admin.getIdUsuario());
    }

    private Usuario usuario(
            String nombre,
            String apellido,
            String dniSufijo,
            String correo,
            RolGlobal rol,
            Genero genero) {

        String dni = dniSufijo.substring(Math.max(0, dniSufijo.length() - 8));
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil perfil(
            String nombreArtistico,
            Usuario usuario,
            Profesion profesion,
            EstadoPerfil estado) {

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-15.")
                .estado(estado)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private record Escenario(
            Long idUsuarioAutor,
            Long idPerfilAutor,
            Long idPerfilReportado,
            Long idUsuarioAdmin) {
    }
}
