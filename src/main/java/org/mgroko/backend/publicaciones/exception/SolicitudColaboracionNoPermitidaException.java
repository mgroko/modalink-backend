package org.mgroko.backend.publicaciones.exception;

/**
 * El perfil activo no es el destinatario de la solicitud de colaboración.
 */
public class SolicitudColaboracionNoPermitidaException extends RuntimeException {

    private final String codigo;

    public SolicitudColaboracionNoPermitidaException(String mensaje) {
        this(mensaje, "SOLICITUD_COLABORACION_NO_PERMITIDA");
    }

    public SolicitudColaboracionNoPermitidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
