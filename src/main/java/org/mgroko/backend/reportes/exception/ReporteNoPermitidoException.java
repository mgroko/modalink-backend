package org.mgroko.backend.reportes.exception;

/**
 * El usuario autenticado no puede emitir reportes (por ejemplo, un
 * administrador).
 */
public class ReporteNoPermitidoException extends RuntimeException {

    public ReporteNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
