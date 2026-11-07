package org.mgroko.backend.auth.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.security.JwtCookieFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

class GoogleOAuth2SuccessHandlerTest {

    private GoogleAuthService googleAuthService;
    private GoogleOAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        googleAuthService = mock(GoogleAuthService.class);
        handler = new GoogleOAuth2SuccessHandler(
                googleAuthService,
                new JwtCookieFactory(false, "Lax"),
                CookieCsrfTokenRepository.withHttpOnlyFalse(),
                "http://localhost:5173");
    }

    @Test
    void loginExistente_emiteJwtYCsrfYRedirigeAlFrontend() throws Exception {
        AuthService.LoginResultado login = new AuthService.LoginResultado(
                "token-jwt",
                new AuthResponse(null));
        when(googleAuthService.autenticar(any(GoogleUserData.class)))
                .thenReturn(GoogleAuthResult.sesion(login, false));

        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "sub-1",
                        "email", "ana@example.com",
                        "email_verified", true,
                        "given_name", "Ana",
                        "family_name", "Gómez"),
                "sub");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                request,
                response,
                new UsernamePasswordAuthenticationToken(oauth2User, null, List.of()));

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().endsWith("/auth/google/callback?status=success"));

        var cookies = response.getHeaders("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("jwt=token-jwt")));
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("XSRF-TOKEN=")));
    }
}
