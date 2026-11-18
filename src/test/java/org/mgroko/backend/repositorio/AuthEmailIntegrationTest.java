package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.TokenEmailException;
import org.mgroko.backend.auth.servicio.AuthEmailService;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.email.TokenAccesoEmailService;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthEmailIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private TokenAccesoEmailService tokenService;
    @Autowired
    private AuthEmailService authEmailService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tokenValido_creaSesionConPerfilContextualYEsDeUnSoloUso() {
        Usuario usuario = crearUsuario("91");
        Perfil perfil = crearPerfil(usuario);

        Notificacion notificacion = notificacionService.crear(
                usuario,
                perfil,
                NotificacionService.TIPO_ACTIVIDAD_ASIGNADA,
                "Actividad asignada",
                "ACTIVIDAD",
                1L);
        notificacion = notificacionService.registrarTodas(List.of(notificacion)).get(0);
        notificacionRepository.flush();

        Long idEntrega = jdbcTemplate.queryForObject(
                "SELECT id_entrega FROM notificacion_entrega WHERE id_notificacion = ?",
                Long.class,
                notificacion.getIdNotificacion());

        String token = tokenService.generarToken(
                idEntrega,
                usuario.getIdUsuario(),
                perfil.getIdPerfil());

        AuthService.LoginResultado resultado = authEmailService.loginConToken(token);

        assertThat(resultado.token()).isNotBlank();
        assertThat(resultado.response().usuario().idUsuario())
                .isEqualTo(usuario.getIdUsuario());
        assertThat(resultado.response().usuario().idPerfilActivo())
                .isEqualTo(perfil.getIdPerfil());

        assertThatThrownBy(() -> authEmailService.loginConToken(token))
                .isInstanceOfSatisfying(TokenEmailException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("TOKEN_USADO"));
    }

    private Usuario crearUsuario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Auth").apellido("Email")
                .dni("96" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("authemail" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());
    }

    private Perfil crearPerfil(Usuario usuario) {
        List<Profesion> profesiones = profesionRepository.findAllByOrderByCodigo();
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil Auth Email")
                .biografia("Perfil de prueba de auto-login")
                .usuario(usuario)
                .profesion(profesiones.get(0))
                .build());
    }
}
