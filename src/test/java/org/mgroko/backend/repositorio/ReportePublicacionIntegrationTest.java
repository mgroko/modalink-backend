package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePublicacionDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePublicacionResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePublicacionService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.reportes.dto.CrearReportePublicacionRequest;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.reportes.servicio.ReportarPublicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class ReportePublicacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ReportarPublicacionService reportarPublicacionService;
    @Autowired
    private AdminReportePublicacionService adminReportePublicacionService;
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
    void reportarPublicacion_persisteReporteAuditoriaYNotificaAlAdmin() {
        Escenario escenario = crearEscenario(EstadoPublicacion.ACTIVA);

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(
                        MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                        "  Contenido ofensivo  "));

        Map<String, Object> reporte = jdbcTemplate.queryForMap("""
                SELECT id_reporte, entidad_tipo, entidad_id, id_usuario_autor,
                       id_perfil_autor, id_perfil_reportado, motivo, descripcion, estado
                FROM reporte
                WHERE id_perfil_autor = ?
                ORDER BY id_reporte DESC
                LIMIT 1
                """, escenario.idPerfilAutor());

        Long idReporte = ((Number) reporte.get("id_reporte")).longValue();
        assertThat(reporte.get("entidad_tipo")).isEqualTo("PUBLICACION");
        assertThat(((Number) reporte.get("entidad_id")).longValue())
                .isEqualTo(escenario.idPublicacion());
        assertThat(((Number) reporte.get("id_usuario_autor")).longValue())
                .isEqualTo(escenario.idUsuarioAutor());
        assertThat(reporte.get("id_perfil_reportado")).isNull();
        assertThat(reporte.get("motivo")).isEqualTo("CONTENIDO_OFENSIVO");
        assertThat(reporte.get("descripcion")).isEqualTo("Contenido ofensivo");
        assertThat(reporte.get("estado")).isEqualTo("PENDIENTE");

        Long auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM auditoria
                WHERE entidad_afectada = 'reporte'
                  AND id_afectado = ?
                """, Long.class, idReporte);
        assertThat(auditorias).isGreaterThanOrEqualTo(1L);

        Long detalles = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM detalle_auditoria d
                JOIN auditoria a ON a.id_auditoria = d.id_auditoria
                WHERE a.entidad_afectada = 'reporte'
                  AND a.id_afectado = ?
                """, Long.class, idReporte);
        assertThat(detalles).isGreaterThanOrEqualTo(4L);

        var notificaciones = notificacionRepository
                .findByUsuarioDestino_IdUsuario(
                        escenario.idUsuarioAdmin(), PageRequest.of(0, 50));

        assertThat(notificaciones.getContent())
                .filteredOn(notificacion -> NotificacionService.TIPO_PUBLICACION_REPORTADA
                        .equals(notificacion.getTipo()))
                .singleElement()
                .satisfies(notificacion -> {
                    assertThat(notificacion.getEntidadTipo())
                            .isEqualTo("REPORTE_PUBLICACION");
                    assertThat(notificacion.getEntidadId()).isEqualTo(idReporte);
                    assertThat(notificacion.getIdProyecto()).isNull();
                    assertThat(notificacion.getPerfilContexto()).isNull();
                    assertThat(notificacion.getMensaje())
                            .contains("Contenido inapropiado")
                            .contains("Contenido ofensivo")
                            .contains("Autor UC69");
                });
    }

    @Test
    void reportarPublicacion_duplicadoPendiente_lanzaConflicto() {
        Escenario escenario = crearEscenario(EstadoPublicacion.ACTIVA);

        CrearReportePublicacionRequest request =
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null);

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                request);

        assertThatThrownBy(() -> reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                request))
                .isInstanceOf(ReportePendienteExistenteException.class);

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reporte WHERE id_perfil_autor = ?
                """, Long.class, escenario.idPerfilAutor());
        assertThat(total).isEqualTo(1L);
    }

    @Test
    void reportarPublicacion_noActiva_lanzaNoEncontrado() {
        Escenario escenario = crearEscenario(EstadoPublicacion.OCULTA);

        assertThatThrownBy(() -> reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null)))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    @Test
    void buscar_admin_devuelveReporteConFiltrosYDetalle() {
        Escenario escenario = crearEscenario(EstadoPublicacion.ACTIVA);

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(
                        MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                        "Contenido inapropiado"));

        Long idReporte = jdbcTemplate.queryForObject("""
                SELECT id_reporte FROM reporte
                WHERE id_perfil_autor = ? ORDER BY id_reporte DESC LIMIT 1
                """, Long.class, escenario.idPerfilAutor());

        PaginaResponse<AdminReportePublicacionResumenResponse> pagina =
                adminReportePublicacionService.buscar(
                        new BuscarReportesPublicacionFiltro(
                                EstadoReporte.PENDIENTE,
                                MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
                                null,
                                null),
                        0, 20, false);

        assertThat(pagina.contenido())
                .filteredOn(fila -> fila.idPerfilAutor().equals(escenario.idPerfilAutor()))
                .singleElement()
                .satisfies(fila -> {
                    assertThat(fila.idReporte()).isEqualTo(idReporte);
                    assertThat(fila.idPublicacion()).isEqualTo(escenario.idPublicacion());
                    assertThat(fila.tituloPublicacion()).startsWith("Publicacion UC69");
                    assertThat(fila.publicacionEliminada()).isFalse();
                    assertThat(fila.motivo())
                            .isEqualTo(MotivoReportePublicacion.CONTENIDO_INAPROPIADO);
                    assertThat(fila.estado()).isEqualTo(EstadoReporte.PENDIENTE);
                });

        AdminReportePublicacionDetalleResponse detalle =
                adminReportePublicacionService.obtenerDetalle(idReporte);

        assertThat(detalle.entidadTipo().name()).isEqualTo("PUBLICACION");
        assertThat(detalle.publicacionEliminada()).isFalse();
        assertThat(detalle.publicacion()).isNotNull();
        assertThat(detalle.publicacion().titulo()).startsWith("Publicacion UC69");
        assertThat(detalle.publicacion().nombreArtistico()).startsWith("Duenio UC69");
        assertThat(detalle.autor().nombreArtistico()).startsWith("Autor UC69");
    }

    @Test
    void actualizarEstado_resuelto_auditaAdminYPermiteNuevoReporte() {
        Escenario escenario = crearEscenario(EstadoPublicacion.ACTIVA);

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null));

        Long idReporte = jdbcTemplate.queryForObject("""
                SELECT id_reporte FROM reporte
                WHERE id_perfil_autor = ? ORDER BY id_reporte DESC LIMIT 1
                """, Long.class, escenario.idPerfilAutor());

        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            String.valueOf(escenario.idUsuarioAdmin()), null, List.of()));

            AdminReportePublicacionDetalleResponse detalle =
                    adminReportePublicacionService.actualizarEstado(
                            idReporte,
                            escenario.idUsuarioAdmin(),
                            new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO));

            assertThat(detalle.estado()).isEqualTo(EstadoReporte.RESUELTO);
            assertThat(detalle.idAdminRevisor()).isEqualTo(escenario.idUsuarioAdmin());
            assertThat(detalle.fechaResolucion()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }

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
                      SELECT id_tipo_auditoria FROM tipo_accion_auditoria
                      WHERE nombre = 'UPDATE'
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

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(
                        MotivoReportePublicacion.OTRO, "Segundo reporte"));

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reporte WHERE id_perfil_autor = ?
                """, Long.class, escenario.idPerfilAutor());
        assertThat(total).isEqualTo(2L);
    }

    @Test
    void publicacionEliminada_conservaReporteYLaBandejaLoTolera() {
        Escenario escenario = crearEscenario(EstadoPublicacion.ACTIVA);

        reportarPublicacionService.reportar(
                escenario.idUsuarioAutor(),
                escenario.idPerfilAutor(),
                escenario.idPublicacion(),
                new CrearReportePublicacionRequest(MotivoReportePublicacion.SPAM, null));

        Long idReporte = jdbcTemplate.queryForObject("""
                SELECT id_reporte FROM reporte
                WHERE id_perfil_autor = ? ORDER BY id_reporte DESC LIMIT 1
                """, Long.class, escenario.idPerfilAutor());

        jdbcTemplate.update(
                "DELETE FROM publicacion WHERE id_publicacion = ?",
                escenario.idPublicacion());

        AdminReportePublicacionDetalleResponse detalle =
                adminReportePublicacionService.obtenerDetalle(idReporte);

        assertThat(detalle.publicacionEliminada()).isTrue();
        assertThat(detalle.publicacion()).isNull();
        assertThat(detalle.entidadId()).isEqualTo(escenario.idPublicacion());

        PaginaResponse<AdminReportePublicacionResumenResponse> pagina =
                adminReportePublicacionService.buscar(
                        new BuscarReportesPublicacionFiltro(
                                EstadoReporte.PENDIENTE,
                                MotivoReportePublicacion.SPAM,
                                null,
                                null),
                        0, 20, false);

        assertThat(pagina.contenido())
                .filteredOn(fila -> fila.idReporte().equals(idReporte))
                .singleElement()
                .satisfies(fila -> {
                    assertThat(fila.publicacionEliminada()).isTrue();
                    assertThat(fila.tituloPublicacion()).isNull();
                });
    }

    private Escenario crearEscenario(EstadoPublicacion estadoPublicacion) {
        return transactionTemplate.execute(status -> {
            String sufijo = String.valueOf(System.nanoTime());
            RolGlobal rolUsuario = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
            RolGlobal rolAdmin = rolGlobalRepository.findByNombre("Administrador").orElseThrow();
            Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
            Profesion profesion = em.find(Profesion.class, 2L);

            Usuario autor = usuario(
                    "Autor", "UC69", sufijo + "1", "uc69.autor." + sufijo,
                    rolUsuario, genero);
            Usuario duenio = usuario(
                    "Duenio", "UC69", sufijo + "2", "uc69.duenio." + sufijo,
                    rolUsuario, genero);
            Usuario admin = usuario(
                    "Admin", "UC69", sufijo + "3", "uc69.admin." + sufijo,
                    rolAdmin, genero);

            Perfil perfilAutor = perfil(
                    "Autor UC69 " + sufijo, autor, profesion);
            Perfil perfilDuenio = perfil(
                    "Duenio UC69 " + sufijo, duenio, profesion);

            Publicacion publicacion = Publicacion.builder()
                    .titulo("Publicacion UC69 " + sufijo)
                    .descripcion("Contenido de la publicacion UC69.")
                    .estado(estadoPublicacion)
                    .perfil(perfilDuenio)
                    .build();
            em.persist(publicacion);
            em.flush();

            return new Escenario(
                    autor.getIdUsuario(),
                    perfilAutor.getIdPerfil(),
                    duenio.getIdUsuario(),
                    perfilDuenio.getIdPerfil(),
                    publicacion.getIdPublicacion(),
                    admin.getIdUsuario());
        });
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
            Profesion profesion) {

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba UC-69.")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private record Escenario(
            Long idUsuarioAutor,
            Long idPerfilAutor,
            Long idUsuarioDuenio,
            Long idPerfilDuenio,
            Long idPublicacion,
            Long idUsuarioAdmin) {
    }
}
