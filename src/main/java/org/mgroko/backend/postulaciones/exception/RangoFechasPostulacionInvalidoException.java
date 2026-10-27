package org.mgroko.backend.postulaciones.exception;

public class RangoFechasPostulacionInvalidoException extends RuntimeException {

    public RangoFechasPostulacionInvalidoException() {
        super("La fecha desde no puede ser posterior a la fecha hasta.");
    }
}
