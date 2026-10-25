package org.mgroko.backend.actividades.exception;

import org.mgroko.backend.modelo.enums.EstadoActividad;

public class TransicionEstadoActividadInvalidaException extends RuntimeException {

    public TransicionEstadoActividadInvalidaException(
            EstadoActividad estadoActual,
            EstadoActividad estadoNuevo) {
        super("No se puede cambiar el estado de la actividad de "
                + estadoActual + " a " + estadoNuevo + ".");
    }
}
