package org.mgroko.backend.conexiones.exception;

/**
 * Ya existe una solicitud de conexión pendiente entre los perfiles, en
 * cualquiera de las dos direcciones (UC-43).
 */
public class SolicitudConexionDuplicadaException extends RuntimeException {

    private final String codigo;

    public SolicitudConexionDuplicadaException(String mensaje) {
        this(mensaje, "SOLICITUD_CONEXION_PENDIENTE");
    }

    public SolicitudConexionDuplicadaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
