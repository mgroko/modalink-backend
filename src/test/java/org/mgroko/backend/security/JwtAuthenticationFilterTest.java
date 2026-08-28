package org.mgroko.backend.security;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * doFilterInternal es protected, por eso este test vive en el mismo
 * paquete (org.mgroko.backend.security) sin necesidad de heredar de
 * la clase. Limpiamos el SecurityContextHolder después de cada test
 * porque es un ThreadLocal estático: si un test lo ensucia, contamina
 * al siguiente.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_sinCookies_continuaCadenaSinAutenticar() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_conCookiesPeroSinJwt_continuaCadenaSinAutenticar() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Cookie otraCookie = new Cookie("otra", "valor");
        when(request.getCookies()).thenReturn(new Cookie[]{otraCookie});

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenValidoYUsuarioActivo_autenticaEnElContexto() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Cookie jwtCookie = new Cookie("jwt", "token-valido");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

        Claims claimsSimulados = mock(Claims.class);
        when(claimsSimulados.getSubject()).thenReturn("123");
        when(jwtService.validarYObtenerClaims("token-valido")).thenReturn(claimsSimulados);

        Usuario usuarioActivo = Usuario.builder()
                .idUsuario(123L)
                .estado(EstadoUsuario.Activo)
                .build();
        when(usuarioRepository.findById(123L)).thenReturn(Optional.of(usuarioActivo));

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("123", authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenInvalido_limpiaContextoYContinuaCadena() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Cookie jwtCookie = new Cookie("jwt", "token-invalido");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtService.validarYObtenerClaims("token-invalido"))
                .thenThrow(new JwtException("Token inválido") {});

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // Clave: aunque el token sea inválido, la cadena de filtros debe
        // seguir (para que otros endpoints públicos no queden bloqueados).
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_usuarioNoExisteEnBD_limpiaContextoYContinuaCadena() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Cookie jwtCookie = new Cookie("jwt", "token-valido");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

        Claims claimsSimulados = mock(Claims.class);
        when(claimsSimulados.getSubject()).thenReturn("999");
        when(jwtService.validarYObtenerClaims("token-valido")).thenReturn(claimsSimulados);
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_usuarioDeshabilitado_limpiaContextoYContinuaCadena() throws Exception {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        Cookie jwtCookie = new Cookie("jwt", "token-valido");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

        Claims claimsSimulados = mock(Claims.class);
        when(claimsSimulados.getSubject()).thenReturn("123");
        when(jwtService.validarYObtenerClaims("token-valido")).thenReturn(claimsSimulados);

        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(123L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(123L)).thenReturn(Optional.of(usuarioDeshabilitado));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
