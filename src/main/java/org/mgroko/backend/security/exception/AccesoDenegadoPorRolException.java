package org.mgroko.backend.security.exception;

/**
 * Se lanza cuando un usuario autenticado intenta operar un área que no
 * corresponde a su rol global (por ejemplo, un administrador en el área de
 * usuario).
 */
public class AccesoDenegadoPorRolException extends RuntimeException {

    public AccesoDenegadoPorRolException() {
        super("Esta acción no está disponible para tu rol.");
    }

    public AccesoDenegadoPorRolException(String message) {
        super(message);
    }
}
