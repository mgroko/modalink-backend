package org.mgroko.backend.actividades.exception;

public class ActividadNoEliminableException extends RuntimeException {

    public ActividadNoEliminableException(String estado) {
        super("No se puede eliminar una actividad en estado " + estado + ".");
    }
}
