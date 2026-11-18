package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificacionEmailOutboxIntegrationTest extends AbstractPostgresIntegrationTest {

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

    @Test
    void notificacionAlta_encolaEntregaEmail() {
        Usuario usuario = crearUsuario("71");
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

        Integer entregas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notificacion_entrega WHERE id_notificacion = ? AND canal = 'EMAIL'",
                Integer.class,
                notificacion.getIdNotificacion());

        assertThat(entregas).isEqualTo(1);
    }

    @Test
    void notificacionMedia_noEncuelaEntregaEmail() {
        Usuario usuario = crearUsuario("72");
        Perfil perfil = crearPerfil(usuario);

        Notificacion notificacion = notificacionService.crear(
                usuario,
                perfil,
                NotificacionService.TIPO_PUBLICACION_COMENTARIO,
                "Nuevo comentario",
                "PUBLICACION",
                1L);
        notificacion = notificacionService.registrarTodas(List.of(notificacion)).get(0);
        notificacionRepository.flush();

        Integer entregas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notificacion_entrega WHERE id_notificacion = ?",
                Integer.class,
                notificacion.getIdNotificacion());

        assertThat(entregas).isZero();
    }

    private Usuario crearUsuario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Email").apellido("Test")
                .dni("98" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("email" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());
    }

    private Perfil crearPerfil(Usuario usuario) {
        List<Profesion> profesiones = profesionRepository.findAllByOrderByCodigo();
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil email")
                .biografia("Perfil de prueba de email")
                .usuario(usuario)
                .profesion(profesiones.get(0))
                .build());
    }
}
