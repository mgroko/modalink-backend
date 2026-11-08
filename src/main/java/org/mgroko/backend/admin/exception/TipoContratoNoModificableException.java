package org.mgroko.backend.admin.exception;

public class TipoContratoNoModificableException extends RuntimeException {

    private final String codigo;

    public TipoContratoNoModificableException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
