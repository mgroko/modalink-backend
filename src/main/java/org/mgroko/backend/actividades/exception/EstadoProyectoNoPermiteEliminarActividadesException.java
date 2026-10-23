package org.mgroko.backend.actividades.exception;

public class EstadoProyectoNoPermiteEliminarActividadesException extends RuntimeException {

    public EstadoProyectoNoPermiteEliminarActividadesException(String estado) {
        super("No se pueden eliminar actividades en un proyecto en estado " + estado + ".");
    }
}
