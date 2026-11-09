package org.mgroko.backend.conexiones.exception;

/**
 * Combinación inválida de filtros de UC-46.
 */
public class FiltroSolicitudConexionInvalidoException extends RuntimeException {

    private final String codigo;

    public FiltroSolicitudConexionInvalidoException(String mensaje) {
        this(mensaje, "FILTRO_INVALIDO");
    }

    public FiltroSolicitudConexionInvalidoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
