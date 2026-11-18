package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
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
import org.mgroko.backend.notificaciones.email.NotificacionEmailScheduler;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.notificaciones.email.habilitado=true")
class NotificacionEmailMailpitIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withInitScript("ModaLinkBD.sql");

    private static final GenericContainer<?> MAILPIT =
            new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
                    .withExposedPorts(1025, 8025);

    static {
        POSTGRES.start();
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

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
    private NotificacionEmailScheduler scheduler;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enviaNotificacionAltaAMailpitYMarchaEntrega() throws Exception {
        String correo = "mailpit@example.com";
        Usuario usuario = crearUsuario("81", correo);
        Perfil perfil = crearPerfil(usuario);

        Notificacion notificacion = notificacionService.crear(
                usuario,
                perfil,
                NotificacionService.TIPO_ACTIVIDAD_ASIGNADA,
                "Se te asignó una actividad",
                "ACTIVIDAD",
                1L);
        notificacion = notificacionService.registrarTodas(List.of(notificacion)).get(0);
        notificacionRepository.flush();

        try {
            limpiarMailpit();
            scheduler.procesarLote();

            JsonNode mensaje = esperarMensajeEnMailpit();
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.path("Subject").asText())
                    .contains("[ModaLink] Actividad asignada");
            assertThat(mensaje.path("To").get(0).path("Address").asText())
                    .isEqualTo(correo);

            String estado = jdbcTemplate.queryForObject(
                    "SELECT estado FROM notificacion_entrega WHERE id_notificacion = ?",
                    String.class,
                    notificacion.getIdNotificacion());
            assertThat(estado).isEqualTo("ENVIADO");
        } finally {
            notificacionRepository.deleteById(notificacion.getIdNotificacion());
            notificacionRepository.flush();
        }
    }

    private void limpiarMailpit() {
        RestClient restClient = mailpitClient();
        try {
            restClient.delete().uri("/api/v1/messages").retrieve().toBodilessEntity();
        } catch (RuntimeException ignored) {
            // La API de limpieza es best-effort.
        }
    }

    private JsonNode esperarMensajeEnMailpit() throws Exception {
        RestClient restClient = mailpitClient();
        ObjectMapper mapper = new ObjectMapper();

        for (int intento = 0; intento < 30; intento++) {
            String json = restClient.get().uri("/api/v1/messages").retrieve().body(String.class);
            JsonNode root = mapper.readTree(json);
            if (root.path("total").asInt() > 0 && root.path("messages").isArray()) {
                return root.path("messages").get(0);
            }
            Thread.sleep(Duration.ofMillis(200).toMillis());
        }
        return null;
    }

    private RestClient mailpitClient() {
        return RestClient.create(
                "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
    }

    private Usuario crearUsuario(String sufijo, String correo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Mailpit").apellido("Test")
                .dni("97" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());
    }

    private Perfil crearPerfil(Usuario usuario) {
        List<Profesion> profesiones = profesionRepository.findAllByOrderByCodigo();
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil Mailpit")
                .biografia("Perfil de prueba de Mailpit")
                .usuario(usuario)
                .profesion(profesiones.get(0))
                .build());
    }
}
