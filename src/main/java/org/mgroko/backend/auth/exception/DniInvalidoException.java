package org.mgroko.backend.auth.exception;

/**
 * Se lanza cuando el DNI enviado no es válido (por ejemplo, contiene
 * caracteres no numéricos o es un número negativo).
 */
public class DniInvalidoException extends RuntimeException {

    public DniInvalidoException(String message) {
        super(message);
    }
}