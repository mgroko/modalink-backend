package org.mgroko.backend.reemplazos.exception;

public class ReemplazoNoGestionableException extends RuntimeException {

    private final String codigo;

    public ReemplazoNoGestionableException(String mensaje) {
        this(mensaje, "REEMPLAZO_NO_GESTIONABLE");
    }

    public ReemplazoNoGestionableException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
