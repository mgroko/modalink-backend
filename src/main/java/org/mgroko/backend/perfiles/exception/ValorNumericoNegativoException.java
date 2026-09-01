package org.mgroko.backend.perfiles.exception;

public class ValorNumericoNegativoException extends RuntimeException {
    public ValorNumericoNegativoException(String mensaje) {
        super(mensaje);
    }
}
