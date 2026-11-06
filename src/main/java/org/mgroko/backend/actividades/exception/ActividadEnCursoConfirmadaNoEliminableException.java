package org.mgroko.backend.actividades.exception;

public class ActividadEnCursoConfirmadaNoEliminableException extends RuntimeException {

    public ActividadEnCursoConfirmadaNoEliminableException() {
        super("No se puede eliminar una actividad en curso de un proyecto confirmado. "
                + "Primero debe cancelarse.");
    }
}
