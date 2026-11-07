package org.mgroko.backend.contratos.exception;

public class ContratoNoModificableException extends RuntimeException {

    private final String codigo;

    public ContratoNoModificableException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
