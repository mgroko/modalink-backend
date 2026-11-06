package org.mgroko.backend.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mgroko.backend.auth.exception.RegistroPendienteInvalidoException;
import org.mgroko.backend.security.JwtService;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Ticket firmado y de vida corta que transporta los datos de Google mientras
 * el usuario completa los datos obligatorios del dominio (DNI, fecha de
 * nacimiento y género).
 */
@Service
public class RegistroPendienteService {

    private static final String PROPOSITO = "google_registro";
    private static final long EXPIRACION_MS = 10 * 60 * 1000L;

    private final JwtService jwtService;

    public RegistroPendienteService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String crearTicket(GoogleUserData datos) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("proposito", PROPOSITO);
        claims.put("sub", datos.sub());
        claims.put("correo", datos.correo());
        claims.put("emailVerificado", datos.emailVerificado());
        claims.put("nombre", datos.nombre());
        claims.put("apellido", datos.apellido());
        claims.put("jti", UUID.randomUUID().toString());

        return jwtService.generarTokenConExpiracion("google-registro", claims, EXPIRACION_MS);
    }

    public GoogleUserData leerTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new RegistroPendienteInvalidoException(
                    "El proceso de registro con Google expiró. Volvé a intentarlo.");
        }

        try {
            Claims claims = jwtService.validarYObtenerClaims(ticket);

            if (!PROPOSITO.equals(claims.get("proposito", String.class))) {
                throw new RegistroPendienteInvalidoException(
                        "El proceso de registro con Google no es válido.");
            }

            String sub = claims.get("sub", String.class);
            String correo = claims.get("correo", String.class);
            Boolean emailVerificado = claims.get("emailVerificado", Boolean.class);

            if (sub == null || sub.isBlank() || correo == null || correo.isBlank()
                    || !Boolean.TRUE.equals(emailVerificado)) {
                throw new RegistroPendienteInvalidoException(
                        "El proceso de registro con Google no es válido.");
            }

            return new GoogleUserData(
                    sub,
                    correo,
                    true,
                    valor(claims.get("nombre")),
                    valor(claims.get("apellido")),
                    null);
        } catch (RegistroPendienteInvalidoException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new RegistroPendienteInvalidoException(
                    "El proceso de registro con Google expiró. Volvé a intentarlo.");
        }
    }

    private String valor(Object valor) {
        return valor != null ? valor.toString() : "";
    }
}
