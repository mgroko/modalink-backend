package org.mgroko.backend.reportes.exception;

/**
 * El reporte ya fue resuelto o descartado y no admite nuevas transiciones.
 */
public class EstadoReporteInvalidoException extends RuntimeException {

    public EstadoReporteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
