package org.mgroko.backend.admin.exception;

public class TipoDatoInvalidoException extends RuntimeException {
    public TipoDatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}