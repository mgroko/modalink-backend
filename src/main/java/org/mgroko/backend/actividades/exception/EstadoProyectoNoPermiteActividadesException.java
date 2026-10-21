package org.mgroko.backend.actividades.exception;

public class EstadoProyectoNoPermiteActividadesException extends RuntimeException {

    public EstadoProyectoNoPermiteActividadesException(String estado) {
        super("No se pueden crear actividades en un proyecto en estado " + estado + ".");
    }
}
