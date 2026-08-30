package org.mgroko.backend.admin.exception;

public class ValorCodigoDuplicadoException extends RuntimeException {
    public ValorCodigoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}