package org.mgroko.backend.publicaciones.exception;

public class RangoFechasPublicacionInvalidoException extends RuntimeException {

    public RangoFechasPublicacionInvalidoException() {
        super("La fecha desde no puede ser posterior a la fecha hasta.");
    }
}
