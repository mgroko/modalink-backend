package org.mgroko.backend.publicaciones.exception;

/**
 * Ya existe una solicitud de colaboración pendiente para la publicación y el
 * destinatario indicados.
 */
public class SolicitudColaboracionDuplicadaException extends RuntimeException {

    private final String codigo;

    public SolicitudColaboracionDuplicadaException(String mensaje) {
        this(mensaje, "SOLICITUD_COLABORACION_PENDIENTE");
    }

    public SolicitudColaboracionDuplicadaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
