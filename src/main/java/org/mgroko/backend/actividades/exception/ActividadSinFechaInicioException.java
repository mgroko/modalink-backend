package org.mgroko.backend.actividades.exception;

public class ActividadSinFechaInicioException extends RuntimeException {

    public ActividadSinFechaInicioException(Long idActividad, String estado) {
        super("La actividad " + idActividad + " no tiene fecha de inicio registrada "
                + "y no se puede pasar al estado " + estado + ".");
    }
}
