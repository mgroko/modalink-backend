package org.mgroko.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * securityFilterChain() es declarativo (wiring de Spring Security) y se
 * valida mejor con un test de integración con MockMvc, no acá. Pero
 * corsConfigurationSource() sí tiene valores concretos que conviene
 * fijar con un test unitario simple, para detectar si alguien cambia
 * el origen permitido sin darse cuenta.
 */
class SecurityConfigTest {

    @Test
    void corsConfigurationSource_devuelveConfiguracionEsperada() {
        SecurityConfig config = new SecurityConfig(mock(JwtService.class));

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfigurations().get("/**");

        assertNotNull(cors);
        assertEquals(List.of("http://localhost:5173"), cors.getAllowedOrigins());
        assertTrue(cors.getAllowedMethods().containsAll(
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
        assertEquals(List.of("*"), cors.getAllowedHeaders());
        assertTrue(cors.getAllowCredentials());
        assertEquals(3600L, cors.getMaxAge());
    }
}