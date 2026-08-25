package org.mgroko.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;

/**
 * Tests de integración del endpoint POST /auth/logout.
 *
 * Estos tests SÍ aplican la cadena de filtros de seguridad (a diferencia de
 * AuthControllerTest que usa addFilters = false). Esto permite verificar el
 * comportamiento completo: filtro JWT → controller → limpieza de cookie/contexto.
 *
 * Estrategia: mockeamos JwtService y UsuarioRepository para controlar lo que
 * el filtro JWT hace al procesar la cookie, sin necesitar una BD real.
 */
@WebMvcTest(controllers = AuthController.class)
class LogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // POST /auth/logout
    // ---------------------------------------------------------------

    @Test
    void logout_sinCookieJwt_devuelve401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_tokenInvalido_devuelve401YContextoVacio() throws Exception {
        when(jwtService.validarYObtenerClaims("token-invalido"))
                .thenThrow(new io.jsonwebtoken.JwtException("Token inválido") {});

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-invalido")))
                .andExpect(status().isUnauthorized());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_usuarioNoExisteEnBD_devuelve401YContextoVacio() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("999");
        when(jwtService.validarYObtenerClaims(anyString())).thenReturn(claims);
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-valido")))
                .andExpect(status().isUnauthorized());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_usuarioDeshabilitado_devuelve401YContextoVacio() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.validarYObtenerClaims(anyString())).thenReturn(claims);

        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .correo("juan@test.com")
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDeshabilitado));

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-valido")))
                .andExpect(status().isUnauthorized());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_usuarioActivo_devuelve200LimpiaCookieYContexto() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.validarYObtenerClaims(anyString())).thenReturn(claims);

        Usuario usuarioActivo = Usuario.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .correo("juan@test.com")
                .estado(EstadoUsuario.Activo)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("jwt", "token-valido")))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cookie = result.getResponse().getHeader("Set-Cookie");
                    assertTrue(cookie.contains("jwt="), "Debe incluir la cookie jwt");
                    assertTrue(cookie.contains("Max-Age=0"), "La cookie debe tener Max-Age=0 para eliminarla");
                    assertTrue(cookie.contains("Path=/"), "La cookie debe tener Path=/");
                });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
