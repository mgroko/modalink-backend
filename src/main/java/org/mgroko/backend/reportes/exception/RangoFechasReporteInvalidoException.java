package org.mgroko.backend.reportes.exception;

/**
 * El rango de fechas del filtro de reportes es inválido.
 */
public class RangoFechasReporteInvalidoException extends RuntimeException {

    public RangoFechasReporteInvalidoException() {
        super("La fecha desde no puede ser posterior a la fecha hasta.");
    }
}
