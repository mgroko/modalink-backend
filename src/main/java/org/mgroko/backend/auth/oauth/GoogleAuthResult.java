package org.mgroko.backend.auth.oauth;

import org.mgroko.backend.auth.servicio.AuthService;

/**
 * Resultado de autenticar con Google: o bien la sesión ya está lista, o bien
 * hace falta completar el registro (ticket de vida corta).
 */
public record GoogleAuthResult(
        AuthService.LoginResultado login,
        String ticket,
        boolean reactivada) {

    public boolean requiereCompletarRegistro() {
        return ticket != null;
    }

    public static GoogleAuthResult sesion(AuthService.LoginResultado login, boolean reactivada) {
        return new GoogleAuthResult(login, null, reactivada);
    }

    public static GoogleAuthResult registroPendiente(String ticket) {
        return new GoogleAuthResult(null, ticket, false);
    }
}
