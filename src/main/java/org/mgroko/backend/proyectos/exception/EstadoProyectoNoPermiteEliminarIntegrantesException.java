package org.mgroko.backend.proyectos.exception;

public class EstadoProyectoNoPermiteEliminarIntegrantesException extends RuntimeException {

    public EstadoProyectoNoPermiteEliminarIntegrantesException(String estado) {
        super("Solo se puede eliminar integrantes en proyectos Publicados o Confirmados. "
                + "Estado actual: " + estado + ".");
    }
}
