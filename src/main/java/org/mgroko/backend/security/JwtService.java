package org.mgroko.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un JWT firmado (JWS) para el usuario indicado.
     * @param subject normalmente el idUsuario o el correo
     * @param claimsExtra claims adicionales (ej. "rol" -> "Usuario")
     */
    public String generarToken(String subject, Map<String, Object> claimsExtra) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claims(claimsExtra)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valida el token y devuelve sus claims. Lanza excepción de jjwt
     * (ExpiredJwtException, SignatureException, etc.) si es inválido.
     */
    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String obtenerSubject(String token) {
        return validarYObtenerClaims(token).getSubject();
    }
}