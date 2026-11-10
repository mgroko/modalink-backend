package org.mgroko.backend.conexiones.exception;

/**
 * La solicitud de conexión no cumple una regla de negocio de UC-43:
 * autorequest o perfiles pertenecientes al mismo usuario.
 */
public class SolicitudConexionInvalidaException extends RuntimeException {

    private final String codigo;

    public SolicitudConexionInvalidaException(String mensaje) {
        this(mensaje, "SOLICITUD_CONEXION_INVALIDA");
    }

    public SolicitudConexionInvalidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
