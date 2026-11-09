package org.mgroko.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class JwtCookieFactoryTest {

    @Test
    void crear_limitaLaCookieALaVidaDelToken() {
        ResponseCookie cookie = new JwtCookieFactory(false, "Lax", 3_600_000L)
                .crear("token");

        assertEquals("jwt", cookie.getName());
        assertEquals(3_600L, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void crearExpirada_borraLaCookie() {
        ResponseCookie cookie = new JwtCookieFactory(false, "Lax", 3_600_000L)
                .crearExpirada();

        assertEquals("jwt", cookie.getName());
        assertEquals(0L, cookie.getMaxAge().getSeconds());
        assertEquals("/", cookie.getPath());
    }
}
