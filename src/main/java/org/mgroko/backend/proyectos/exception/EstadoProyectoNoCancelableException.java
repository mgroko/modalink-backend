package org.mgroko.backend.proyectos.exception;

public class EstadoProyectoNoCancelableException extends RuntimeException {

    public EstadoProyectoNoCancelableException(String estadoActual) {
        super("Solo se puede cancelar un proyecto en estado Publicado o Confirmado. Estado actual: "
                + estadoActual + ".");
    }
}
