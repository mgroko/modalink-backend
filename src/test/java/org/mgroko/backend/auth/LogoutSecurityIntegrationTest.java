package org.mgroko.backend.auth;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.controlador.AuthController;
import org.mgroko.backend.auth.oauth.CookieOAuth2AuthorizationRequestRepository;
import org.mgroko.backend.auth.oauth.GoogleAuthService;
import org.mgroko.backend.auth.oauth.GoogleOAuth2FailureHandler;
import org.mgroko.backend.auth.oauth.GoogleOAuth2SuccessHandler;
import org.mgroko.backend.auth.oauth.RegistroPendienteService;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.CsrfConfig;
import org.mgroko.backend.security.JwtCookieFactory;
import org.mgroko.backend.security.JwtService;
import org.mgroko.backend.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración real de {@code POST /auth/logout} con la cadena de seguridad
 * activa (filtros incluidos).
 *
 * <p>{@link LogoutIntegrationTest} prueba el controlador aislado
 * ({@code addFilters = false}); por eso no detectaba que Spring Security
 * podía responder 401/403 antes de llegar al controlador y dejar la cookie
 * JWT viva.</p>
 */
@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, CsrfConfig.class})
class LogoutSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtCookieFactory jwtCookieFactory;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @MockitoBean
    private RegistroPendienteService registroPendienteService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @MockitoBean
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @MockitoBean
    private GoogleOAuth2FailureHandler googleOAuth2FailureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private void cookieExpirada() {
        when(jwtCookieFactory.crearExpirada())
                .thenReturn(ResponseCookie.from("jwt", "")
                        .maxAge(0)
                        .path("/")
                        .build());
    }

    @Test
    void logout_sinSesionNiCsrf_limpiaLaCookie() throws Exception {
        cookieExpirada();

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_conJwtInvalidoNiCsrf_limpiaLaCookie() throws Exception {
        cookieExpirada();
        when(jwtService.validarYObtenerClaims(anyString()))
                .thenThrow(new JwtException("JWT inválido") {
                });

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-invalido")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_conSesionValidaSinCsrf_limpiaLaCookie() throws Exception {
        cookieExpirada();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.validarYObtenerClaims("token-valido")).thenReturn(claims);

        RolGlobal rol = RolGlobal.builder()
                .idRolGlobal(1L)
                .codigo("USUARIO")
                .nombre("Usuario")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("usuario@test.com")
                .estado(EstadoUsuario.ACTIVO)
                .rolGlobal(rol)
                .build();
        when(usuarioRepository.findByIdConRolYPermisos(1L))
                .thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-valido")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
}
