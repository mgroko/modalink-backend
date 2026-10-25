package org.mgroko.backend.proyectos.exception;

public class EstadoProyectoNoModificableException extends RuntimeException {

    public EstadoProyectoNoModificableException(String estadoActual) {
        super("No se puede modificar un proyecto en estado " + estadoActual + ".");
    }
}
