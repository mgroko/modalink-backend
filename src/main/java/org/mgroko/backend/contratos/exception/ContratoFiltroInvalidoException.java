package org.mgroko.backend.contratos.exception;

public class ContratoFiltroInvalidoException extends RuntimeException {

    private final String codigo;

    public ContratoFiltroInvalidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
