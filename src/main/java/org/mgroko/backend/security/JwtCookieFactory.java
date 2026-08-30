package org.mgroko.backend.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieFactory {

    private static final String NOMBRE_COOKIE = "jwt";
    private static final int MAX_AGE_SEGUNDOS = 24 * 60 * 60;

    public ResponseCookie crear(String token) {
        return ResponseCookie.from(NOMBRE_COOKIE, token)
                .httpOnly(true)
                .secure(false) // false para desarrollo, en producción debe ser true
                .path("/")
                .maxAge(MAX_AGE_SEGUNDOS)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie crearExpirada() {
        return ResponseCookie.from(NOMBRE_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
