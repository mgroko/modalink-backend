package org.mgroko.backend.reportes.exception;

/**
 * La descripción supera la longitud permitida.
 */
public class DescripcionReporteInvalidaException extends RuntimeException {

    public DescripcionReporteInvalidaException(String mensaje) {
        super(mensaje);
    }
}
