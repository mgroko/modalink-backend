package org.mgroko.backend.conexiones.exception;

/**
 * El perfil activo no puede gestionar la solicitud indicada (UC-45).
 */
public class SolicitudConexionNoPermitidaException extends RuntimeException {

    private final String codigo;

    public SolicitudConexionNoPermitidaException(String mensaje) {
        this(mensaje, "SOLICITUD_CONEXION_NO_PERMITIDA");
    }

    public SolicitudConexionNoPermitidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
