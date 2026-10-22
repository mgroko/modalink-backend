package org.mgroko.backend.actividades.exception;

public class ActividadNoEncontradaException extends RuntimeException {

    public ActividadNoEncontradaException(Long idActividad) {
        super("La actividad " + idActividad + " no existe o no pertenece al proyecto indicado.");
    }
}
