package org.mgroko.backend.mensajeria.exception;

/**
 * No se puede iniciar una conversación: mismo usuario, sin conexión activa o
 * perfiles/usuarios inactivos.
 */
public class ConversacionNoPermitidaException extends RuntimeException {

    private final String codigo;

    public ConversacionNoPermitidaException(String mensaje) {
        this(mensaje, "CONVERSACION_NO_PERMITIDA");
    }

    public ConversacionNoPermitidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
