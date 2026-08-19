package org.mgroko.backend.security;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

/**
 * JwtService no depende de Spring context: se instancia directo con
 * los mismos parámetros que recibiría por @Value en producción.
 * El secret debe tener al menos 32 caracteres (256 bits) porque
 * Keys.hmacShaKeyFor exige esa longitud mínima para HS256.
 */
class JwtServiceTest {

    private static final String SECRET_DE_TEST = "clave-secreta-de-test-minimo-32-caracteres!!";

    @Test
    void generarYValidarToken_datosValidos_devuelveClaimsCorrectos() {
        JwtService jwtService = new JwtService(SECRET_DE_TEST, 60_000L); // 1 minuto

        String token = jwtService.generarToken("123", Map.of("correo", "juan@test.com"));

        var claims = jwtService.validarYObtenerClaims(token);
        assertEquals("123", claims.getSubject());
        assertEquals("juan@test.com", claims.get("correo"));
    }

    @Test
    void obtenerSubject_delegaEnValidarYObtenerClaims() {
        JwtService jwtService = new JwtService(SECRET_DE_TEST, 60_000L);
        String token = jwtService.generarToken("456", Map.of());

        assertEquals("456", jwtService.obtenerSubject(token));
    }

    @Test
    void validarToken_expirado_lanzaExpiredJwtException() throws InterruptedException {
        // expirationMs negativo -> el token nace ya vencido
        JwtService jwtService = new JwtService(SECRET_DE_TEST, -1000L);
        String token = jwtService.generarToken("789", Map.of());

        assertThrows(ExpiredJwtException.class, () -> jwtService.validarYObtenerClaims(token));
    }

    @Test
    void validarToken_firmaManipulada_lanzaSignatureException() {
        JwtService jwtService = new JwtService(SECRET_DE_TEST, 60_000L);
        String token = jwtService.generarToken("123", Map.of());

        // Alteramos un carácter del payload (segunda sección del JWT) para
        // que la firma ya no coincida con el contenido.
        String[] partes = token.split("\\.");
        char ultimoCaracter = partes[1].charAt(partes[1].length() - 1);
        char reemplazo = ultimoCaracter == 'a' ? 'b' : 'a';
        String payloadAlterado = partes[1].substring(0, partes[1].length() - 1) + reemplazo;
        String tokenManipulado = partes[0] + "." + payloadAlterado + "." + partes[2];

        assertThrows(SignatureException.class, () -> jwtService.validarYObtenerClaims(tokenManipulado));
    }

    @Test
    void validarToken_formatoInvalido_lanzaMalformedJwtException() {
        JwtService jwtService = new JwtService(SECRET_DE_TEST, 60_000L);

        assertThrows(MalformedJwtException.class, () -> jwtService.validarYObtenerClaims("esto-no-es-un-jwt"));
    }

    @Test
    void generarToken_devuelveTokenNoNuloConTresSegmentos() {
        JwtService jwtService = new JwtService(SECRET_DE_TEST, 60_000L);
        String token = jwtService.generarToken("123", Map.of());

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length); // header.payload.firma
    }
}