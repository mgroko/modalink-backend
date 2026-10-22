package org.mgroko.backend.actividades.exception;

public class EstadoProyectoNoPermiteModificarActividadesException extends RuntimeException {

    public EstadoProyectoNoPermiteModificarActividadesException(String estado) {
        super("No se pueden modificar actividades en un proyecto en estado " + estado + ".");
    }
}
