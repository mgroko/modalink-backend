package org.mgroko.backend.perfiles.exception;

public class ValorObligatorioException extends RuntimeException {
    public ValorObligatorioException(String mensaje) {
        super(mensaje);
    }
}