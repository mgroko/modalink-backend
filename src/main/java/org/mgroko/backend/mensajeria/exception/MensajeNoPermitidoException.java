package org.mgroko.backend.mensajeria.exception;

/**
 * La conversación existe pero ya no admite mensajes porque la conexión dejó
 * de estar activa o algún participante dejó de estar activo.
 */
public class MensajeNoPermitidoException extends RuntimeException {

    private final String codigo;

    public MensajeNoPermitidoException(String mensaje) {
        this(mensaje, "MENSAJE_NO_PERMITIDO");
    }

    public MensajeNoPermitidoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
