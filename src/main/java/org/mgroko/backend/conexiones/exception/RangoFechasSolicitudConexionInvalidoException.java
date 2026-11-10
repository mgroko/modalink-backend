package org.mgroko.backend.conexiones.exception;

/**
 * El rango de fechas de UC-46 es inválido: {@code fechaDesde} es posterior a
 * {@code fechaHasta}.
 */
public class RangoFechasSolicitudConexionInvalidoException extends RuntimeException {

    private final String codigo;

    public RangoFechasSolicitudConexionInvalidoException() {
        this("La fecha desde no puede ser posterior a la fecha hasta.",
                "RANGO_FECHAS_INVALIDO");
    }

    public RangoFechasSolicitudConexionInvalidoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
