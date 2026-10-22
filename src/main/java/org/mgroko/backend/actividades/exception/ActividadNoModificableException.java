package org.mgroko.backend.actividades.exception;

public class ActividadNoModificableException extends RuntimeException {

    public ActividadNoModificableException(String estado) {
        super("No se puede modificar una actividad en estado " + estado + ".");
    }
}
