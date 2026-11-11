package org.mgroko.backend.publicaciones.exception;

/**
 * Rango de fechas inválido en la búsqueda de solicitudes de colaboración.
 */
public class RangoFechasSolicitudColaboracionInvalidoException extends RuntimeException {

    public RangoFechasSolicitudColaboracionInvalidoException() {
        super("La fecha desde no puede ser posterior a la fecha hasta.");
    }
}
