package org.mgroko.backend.mensajeria.exception;

/**
 * El contenido del mensaje no cumple las reglas de longitud.
 */
public class MensajeInvalidoException extends RuntimeException {

    private final String codigo;

    public MensajeInvalidoException(String mensaje) {
        this(mensaje, "MENSAJE_INVALIDO");
    }

    public MensajeInvalidoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
