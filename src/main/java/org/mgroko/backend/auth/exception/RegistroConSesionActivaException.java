package org.mgroko.backend.auth.exception;

/**
 * Se lanza cuando se intenta registrar una cuenta con una sesión ya
 * autenticada. Evita que una sesión de administrador se transforme en una
 * sesión de usuario sin cerrar sesión previamente.
 */
public class RegistroConSesionActivaException extends RuntimeException {

    public RegistroConSesionActivaException() {
        super("Ya tenés una sesión activa. Cerrá sesión antes de registrar otra cuenta.");
    }
}
