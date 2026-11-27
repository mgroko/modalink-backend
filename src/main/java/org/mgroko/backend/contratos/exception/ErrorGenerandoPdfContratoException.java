package org.mgroko.backend.contratos.exception;

/**
 * Error al componer el PDF de un contrato.
 */
public class ErrorGenerandoPdfContratoException extends RuntimeException {

    public ErrorGenerandoPdfContratoException(String message, Throwable cause) {
        super(message, cause);
    }
}
