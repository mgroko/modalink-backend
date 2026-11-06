package org.mgroko.backend.auth.oauth;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Datos mínimos que Google devuelve y que usa el flujo de UC-09.
 */
public record GoogleUserData(
        String sub,
        String correo,
        boolean emailVerificado,
        String nombre,
        String apellido,
        String fotoUrl) {

    public static GoogleUserData desde(OAuth2User user) {
        Map<String, Object> atributos = user.getAttributes();

        String sub = texto(atributos.get("sub"));
        String correo = texto(atributos.get("email"));
        if (sub == null || sub.isBlank() || correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("Google no devolvió el identificador o el correo del usuario.");
        }

        String nombre = texto(atributos.get("given_name"));
        String apellido = texto(atributos.get("family_name"));
        if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank()) {
            String nombreCompleto = texto(atributos.get("name"));
            if (nombreCompleto != null && !nombreCompleto.isBlank()) {
                int espacio = nombreCompleto.indexOf(' ');
                if (espacio > 0) {
                    if (nombre == null || nombre.isBlank()) {
                        nombre = nombreCompleto.substring(0, espacio).trim();
                    }
                    if (apellido == null || apellido.isBlank()) {
                        apellido = nombreCompleto.substring(espacio + 1).trim();
                    }
                } else if (nombre == null || nombre.isBlank()) {
                    nombre = nombreCompleto.trim();
                }
            }
        }

        return new GoogleUserData(
                sub.trim(),
                correo.trim().toLowerCase(),
                booleano(atributos.get("email_verified")),
                nombre != null ? nombre.trim() : "",
                apellido != null ? apellido.trim() : "",
                texto(atributos.get("picture")));
    }

    private static String texto(Object valor) {
        return valor != null ? valor.toString() : null;
    }

    private static boolean booleano(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        return valor != null && Boolean.parseBoolean(valor.toString());
    }
}
