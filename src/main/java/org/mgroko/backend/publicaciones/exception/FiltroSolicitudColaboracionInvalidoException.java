package org.mgroko.backend.publicaciones.exception;

/**
 * Combinación inválida de filtros de UC-65.
 */
public class FiltroSolicitudColaboracionInvalidoException extends RuntimeException {

    public FiltroSolicitudColaboracionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
