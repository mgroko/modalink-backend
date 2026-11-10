package org.mgroko.backend.conexiones.exception;

/**
 * La conexión existe pero ya fue eliminada.
 */
public class ConexionNoActivaException extends RuntimeException {

    private final String codigo;

    public ConexionNoActivaException(String mensaje) {
        this(mensaje, "CONEXION_NO_ACTIVA");
    }

    public ConexionNoActivaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
