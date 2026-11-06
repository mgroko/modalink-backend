package org.mgroko.backend.actividades.exception;

public class ActividadCanceladaException extends RuntimeException {

    public ActividadCanceladaException(Long idActividad) {
        super("La actividad " + idActividad + " está cancelada y no admite consultas de disponibilidad.");
    }
}
