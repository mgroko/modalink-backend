package org.mgroko.backend.publicaciones.exception;

/**
 * La solicitud de colaboración no cumple una regla de negocio de UC-61.
 */
public class SolicitudColaboracionInvalidaException extends RuntimeException {

    private final String codigo;

    public SolicitudColaboracionInvalidaException(String mensaje) {
        this(mensaje, "SOLICITUD_COLABORACION_INVALIDA");
    }

    public SolicitudColaboracionInvalidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
