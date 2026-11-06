package org.mgroko.backend.reemplazos.exception;

public class ReemplazoNoPermitidoException extends RuntimeException {

    private final String codigo;

    public ReemplazoNoPermitidoException(String mensaje) {
        this(mensaje, "REEMPLAZO_NO_PERMITIDO");
    }

    public ReemplazoNoPermitidoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
