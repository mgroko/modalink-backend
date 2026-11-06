package org.mgroko.backend.auth.oauth;

import java.io.IOException;

import org.mgroko.backend.security.JwtCookieFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    private final GoogleAuthService googleAuthService;
    private final JwtCookieFactory jwtCookieFactory;
    private final String frontendUrl;

    public GoogleOAuth2SuccessHandler(
            GoogleAuthService googleAuthService,
            JwtCookieFactory jwtCookieFactory,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.googleAuthService = googleAuthService;
        this.jwtCookieFactory = jwtCookieFactory;
        this.frontendUrl = quitarBarraFinal(frontendUrl);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            GoogleUserData datos = GoogleUserData.desde(oauth2User);
            GoogleAuthResult resultado = googleAuthService.autenticar(datos);

            if (resultado.requiereCompletarRegistro()) {
                agregarCookie(response, jwtCookieFactory.crearRegistroPendiente(resultado.ticket()));
                response.sendRedirect(frontendUrl + "/auth/google/completar");
                return;
            }

            agregarCookie(response, jwtCookieFactory.crear(resultado.login().token()));
            String destino = frontendUrl + "/auth/google/callback?status=success";
            if (resultado.reactivada()) {
                destino += "&reactivada=1";
            }
            response.sendRedirect(destino);
        } catch (RuntimeException ex) {
            // No se loguean datos personales ni tokens.
            log.warn("Falló el inicio de sesión con Google: {}", ex.getClass().getSimpleName());
            response.sendRedirect(frontendUrl + "/login?googleError=google_auth_failed");
        }
    }

    private void agregarCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String quitarBarraFinal(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
