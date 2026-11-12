package org.mgroko.backend.auth.controlador;

import org.mgroko.backend.auth.dto.AuthResponse;
import org.mgroko.backend.auth.dto.ValidarTokenEmailRequest;
import org.mgroko.backend.auth.exception.TokenEmailException;
import org.mgroko.backend.auth.servicio.AuthEmailService;
import org.mgroko.backend.auth.servicio.AuthService;
import org.mgroko.backend.security.JwtCookieFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Intercambia el token de un email por una sesión JWT.
 *
 * <p>Es público y está exento de CSRF porque el usuario todavía no tiene
 * sesión ni token CSRF. El token de un solo uso es la credencial.</p>
 */
@RestController
@RequestMapping("/auth/email")
public class AuthEmailController {

    private final AuthEmailService authEmailService;
    private final JwtCookieFactory jwtCookieFactory;

    public AuthEmailController(
            AuthEmailService authEmailService,
            JwtCookieFactory jwtCookieFactory) {
        this.authEmailService = authEmailService;
        this.jwtCookieFactory = jwtCookieFactory;
    }

    @PostMapping("/validar")
    public ResponseEntity<AuthResponse> validar(
            @RequestBody(required = false) ValidarTokenEmailRequest request) {

        if (request == null) {
            throw TokenEmailException.requerido();
        }

        AuthService.LoginResultado resultado =
                authEmailService.loginConToken(request.token());

        ResponseCookie cookie = jwtCookieFactory.crear(resultado.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Referrer-Policy", "no-referrer")
                .body(resultado.response());
    }
}
