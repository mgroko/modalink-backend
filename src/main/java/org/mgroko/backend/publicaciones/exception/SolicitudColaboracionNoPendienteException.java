package org.mgroko.backend.publicaciones.exception;

/**
 * La solicitud de colaboración ya fue aceptada o rechazada.
 */
public class SolicitudColaboracionNoPendienteException extends RuntimeException {

    private final String codigo;

    public SolicitudColaboracionNoPendienteException(String mensaje) {
        this(mensaje, "SOLICITUD_COLABORACION_NO_PENDIENTE");
    }

    public SolicitudColaboracionNoPendienteException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
