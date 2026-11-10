package org.mgroko.backend.conexiones.exception;

/**
 * Ya existe una conexión vigente entre los perfiles (UC-43 y UC-45).
 */
public class ConexionYaExistenteException extends RuntimeException {

    private final String codigo;

    public ConexionYaExistenteException(String mensaje) {
        this(mensaje, "CONEXION_YA_EXISTENTE");
    }

    public ConexionYaExistenteException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
