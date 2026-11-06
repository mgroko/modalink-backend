package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResumenResponse;
import org.mgroko.backend.notificaciones.exception.NotificacionNoEncontradaException;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificacionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String TIPO = "HITO_CUMPLIDO";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario crearUsuario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Notificacion").apellido("Test")
                .dni("99" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("notif" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());
    }

    private Perfil crearPerfil(Usuario usuario, int indiceProfesion, String nombre) {
        List<Profesion> profesiones = profesionRepository.findAllByOrderByCodigo();
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de prueba de notificaciones")
                .usuario(usuario)
                .profesion(profesiones.get(indiceProfesion))
                .build());
    }

    private Notificacion crearNotificacion(
            Usuario usuario, Perfil perfil, String mensaje, LocalDateTime fechaGenerada) {
        Notificacion notificacion = notificacionService.crear(
                usuario, perfil, TIPO, mensaje, "HITO", 1L);
        notificacion.setFechaGenerada(fechaGenerada);
        return notificacionService.registrarTodas(List.of(notificacion)).get(0);
    }

    private List<String> mensajesDe(PaginaResponse<NotificacionResponse> pagina) {
        return pagina.contenido().stream().map(NotificacionResponse::mensaje).toList();
    }

    @Test
    void listarYResumen_aislanPorUsuarioYPerfil() {
        Usuario usuarioA = crearUsuario("01");
        Usuario usuarioB = crearUsuario("02");
        Perfil perfilA1 = crearPerfil(usuarioA, 0, "Perfil A1");
        Perfil perfilA2 = crearPerfil(usuarioA, 1, "Perfil A2");
        Perfil perfilB = crearPerfil(usuarioB, 0, "Perfil B");

        crearNotificacion(usuarioA, perfilA1, "A1", LocalDateTime.of(2026, 1, 1, 10, 0));
        crearNotificacion(usuarioA, perfilA2, "A2", LocalDateTime.of(2026, 1, 2, 10, 0));
        crearNotificacion(usuarioA, null, "A-null", LocalDateTime.of(2026, 1, 3, 10, 0));
        crearNotificacion(usuarioB, perfilB, "B1", LocalDateTime.of(2026, 1, 4, 10, 0));
        notificacionRepository.flush();

        PaginaResponse<NotificacionResponse> todasDeA =
                notificacionService.listar(usuarioA.getIdUsuario(), null, 0, 20);
        PaginaResponse<NotificacionResponse> soloPerfilA1 =
                notificacionService.listar(usuarioA.getIdUsuario(), perfilA1.getIdPerfil(), 0, 20);

        assertThat(mensajesDe(todasDeA)).containsExactly("A-null", "A2", "A1");
        assertThat(mensajesDe(soloPerfilA1)).containsExactly("A1");

        NotificacionResumenResponse resumenA =
                notificacionService.resumen(usuarioA.getIdUsuario(), null);
        NotificacionResumenResponse resumenA2 =
                notificacionService.resumen(usuarioA.getIdUsuario(), perfilA2.getIdPerfil());
        NotificacionResumenResponse resumenB =
                notificacionService.resumen(usuarioB.getIdUsuario(), null);

        assertThat(resumenA.total()).isEqualTo(3);
        assertThat(resumenA.noLeidas()).isEqualTo(3);
        assertThat(resumenA2.total()).isEqualTo(1);
        assertThat(resumenA2.noLeidas()).isEqualTo(1);
        assertThat(resumenB.total()).isEqualTo(1);
    }

    @Test
    void listar_proyectoCancelado_derivaIdProyectoDesdeEntidadId() {
        Usuario usuario = crearUsuario("09");
        Perfil perfil = crearPerfil(usuario, 0, "Perfil Proyecto");

        Notificacion notificacion = notificacionService.crear(
                usuario, perfil, "PROYECTO_CANCELADO", "Proyecto cancelado", "PROYECTO", 55L);
        notificacionService.registrarTodas(List.of(notificacion));

        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(
                usuario.getIdUsuario(), perfil.getIdPerfil(), 0, 20);

        assertThat(pagina.contenido()).singleElement()
                .satisfies(n -> assertThat(n.idProyecto()).isEqualTo(55L));
    }

    @Test
    void listar_actividadEliminada_usaIdProyectoPersistidoAunqueLaActividadNoExista() {
        Usuario usuario = crearUsuario("10");
        Perfil perfil = crearPerfil(usuario, 0, "Perfil Actividad Eliminada");

        Notificacion notificacion = notificacionService.crear(
                usuario,
                perfil,
                NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                "La actividad fue eliminada.",
                "ACTIVIDAD",
                999_999L,
                55L);
        notificacionService.registrarTodas(List.of(notificacion));

        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(
                usuario.getIdUsuario(), perfil.getIdPerfil(), 0, 20);

        assertThat(pagina.contenido()).singleElement().satisfies(n -> {
            assertThat(n.titulo()).isEqualTo("Actividad eliminada");
            assertThat(n.idProyecto()).isEqualTo(55L);
            assertThat(n.idActividad()).isEqualTo(999_999L);
            assertThat(n.idHito()).isNull();
            assertThat(n.idInvitacion()).isNull();
        });
    }

    @Test
    void marcarLeidaYMarcarTodas_respetanUsuarioYPerfil() {
        Usuario usuarioA = crearUsuario("03");
        Usuario usuarioB = crearUsuario("04");
        Perfil perfilA1 = crearPerfil(usuarioA, 0, "Perfil A1");
        Perfil perfilA2 = crearPerfil(usuarioA, 1, "Perfil A2");
        Perfil perfilB = crearPerfil(usuarioB, 0, "Perfil B");

        Notificacion a1 = crearNotificacion(
                usuarioA, perfilA1, "A1", LocalDateTime.of(2026, 2, 1, 10, 0));
        crearNotificacion(usuarioA, perfilA2, "A2", LocalDateTime.of(2026, 2, 2, 10, 0));
        crearNotificacion(usuarioA, null, "A-null", LocalDateTime.of(2026, 2, 3, 10, 0));
        crearNotificacion(usuarioB, perfilB, "B1", LocalDateTime.of(2026, 2, 4, 10, 0));
        notificacionRepository.flush();

        NotificacionResponse marcada =
                notificacionService.marcarLeida(usuarioA.getIdUsuario(), a1.getIdNotificacion());
        assertThat(marcada.leida()).isTrue();
        assertThat(marcada.fechaLeida()).isNotNull();
        assertThat(notificacionService.resumen(usuarioA.getIdUsuario(), null).noLeidas())
                .isEqualTo(2);

        int marcadasPerfilA2 = notificacionService.marcarTodasLeidas(
                usuarioA.getIdUsuario(), perfilA2.getIdPerfil());
        assertThat(marcadasPerfilA2).isEqualTo(1);
        assertThat(notificacionService.resumen(usuarioA.getIdUsuario(), perfilA2.getIdPerfil())
                .noLeidas()).isZero();
        assertThat(notificacionService.resumen(usuarioA.getIdUsuario(), null).noLeidas())
                .isEqualTo(1);

        int marcadasRestantes = notificacionService.marcarTodasLeidas(
                usuarioA.getIdUsuario(), null);
        assertThat(marcadasRestantes).isEqualTo(1);
        assertThat(notificacionService.resumen(usuarioA.getIdUsuario(), null).noLeidas()).isZero();

        assertThat(notificacionService.resumen(usuarioB.getIdUsuario(), null).noLeidas())
                .isEqualTo(1);
    }

    @Test
    void marcarLeida_ajena_lanzaExcepcion() {
        Usuario usuarioA = crearUsuario("05");
        Usuario usuarioB = crearUsuario("06");
        Perfil perfilA = crearPerfil(usuarioA, 0, "Perfil A");
        Perfil perfilB = crearPerfil(usuarioB, 0, "Perfil B");
        Notificacion notificacionB = crearNotificacion(
                usuarioB, perfilB, "B1", LocalDateTime.of(2026, 3, 1, 10, 0));
        notificacionRepository.flush();

        assertThatThrownBy(() -> notificacionService.marcarLeida(
                usuarioA.getIdUsuario(), notificacionB.getIdNotificacion()))
                .isInstanceOf(NotificacionNoEncontradaException.class);

        assertThat(notificacionService.resumen(usuarioB.getIdUsuario(), perfilB.getIdPerfil())
                .noLeidas()).isEqualTo(1);
    }

    @Test
    void insercion_conPerfilDeOtroUsuario_esRechazadaPorTrigger() {
        Usuario usuarioA = crearUsuario("07");
        Usuario usuarioB = crearUsuario("08");
        Perfil perfilB = crearPerfil(usuarioB, 0, "Perfil B");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO notificacion
                    (tipo, mensaje, leida, origen, id_usuario_destino, id_perfil_contexto)
                VALUES (?, 'Contexto ajeno', false, 'SERVICIO', ?, ?)
                """, TIPO, usuarioA.getIdUsuario(), perfilB.getIdPerfil()))
                .isInstanceOf(DataAccessException.class);
    }
}
