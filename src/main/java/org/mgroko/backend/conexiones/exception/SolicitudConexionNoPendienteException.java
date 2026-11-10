package org.mgroko.backend.conexiones.exception;

/**
 * Sólo se pueden cancelar solicitudes de conexión en estado PENDIENTE.
 */
public class SolicitudConexionNoPendienteException extends RuntimeException {

    private final String codigo;

    public SolicitudConexionNoPendienteException(String mensaje) {
        this(mensaje, "SOLICITUD_CONEXION_NO_PENDIENTE");
    }

    public SolicitudConexionNoPendienteException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
