package org.mgroko.backend.reportes.exception;

/**
 * El reporte no existe o no es un reporte de perfil.
 */
public class ReporteNoEncontradoException extends RuntimeException {

    public ReporteNoEncontradoException() {
        super("El reporte no existe.");
    }
}
