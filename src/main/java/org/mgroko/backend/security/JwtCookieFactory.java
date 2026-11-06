package org.mgroko.backend.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieFactory {

    public static final String NOMBRE_COOKIE = "jwt";
    public static final String NOMBRE_COOKIE_REGISTRO_GOOGLE = "google_registro";

    private static final int MAX_AGE_SEGUNDOS = 24 * 60 * 60;
    private static final Duration MAX_AGE_REGISTRO_GOOGLE = Duration.ofMinutes(10);

    private final boolean secure;
    private final String sameSite;

    public JwtCookieFactory(
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie crear(String token) {
        return base(NOMBRE_COOKIE, token)
                .maxAge(MAX_AGE_SEGUNDOS)
                .build();
    }

    public ResponseCookie crearExpirada() {
        return base(NOMBRE_COOKIE, "")
                .maxAge(0)
                .build();
    }

    /**
     * Ticket de vida corta que transporta los datos de Google mientras el
     * usuario completa DNI, fecha de nacimiento y género.
     */
    public ResponseCookie crearRegistroPendiente(String token) {
        return base(NOMBRE_COOKIE_REGISTRO_GOOGLE, token)
                .maxAge(MAX_AGE_REGISTRO_GOOGLE)
                .build();
    }

    public ResponseCookie crearRegistroPendienteExpirada() {
        return base(NOMBRE_COOKIE_REGISTRO_GOOGLE, "")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String nombre, String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite);
    }
}
