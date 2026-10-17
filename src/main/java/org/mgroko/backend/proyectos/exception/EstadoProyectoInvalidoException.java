package org.mgroko.backend.proyectos.exception;

public class EstadoProyectoInvalidoException extends RuntimeException {

    public EstadoProyectoInvalidoException(String estadoActual) {
        super("Solo se puede publicar un proyecto en estado Borrador. Estado actual: " + estadoActual + ".");
    }
}
