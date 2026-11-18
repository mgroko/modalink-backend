package org.mgroko.backend.repositorio;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthEmailControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
    private JdbcTemplate jdbcTemplate;

    @Test
    void validaTokenSinSesionPreviaYDevuelveCookieJwt() throws Exception {
        Usuario usuario = crearUsuario("95");
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

        mockMvc.perform(post("/auth/email/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(jsonPath("$.usuario.idUsuario").value(usuario.getIdUsuario()))
                .andExpect(jsonPath("$.usuario.idPerfilActivo").value(perfil.getIdPerfil()));
    }

    @Test
    void tokenInvalidoDevuelveErrorControlado() throws Exception {
        mockMvc.perform(post("/auth/email/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "no-existe"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    private Usuario crearUsuario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Auth").apellido("Controller")
                .dni("95" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("authcontroller" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());
    }

    private Perfil crearPerfil(Usuario usuario) {
        List<Profesion> profesiones = profesionRepository.findAllByOrderByCodigo();
        return perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil Auth Controller")
                .biografia("Perfil de prueba de endpoint de email")
                .usuario(usuario)
                .profesion(profesiones.get(0))
                .build());
    }
}
