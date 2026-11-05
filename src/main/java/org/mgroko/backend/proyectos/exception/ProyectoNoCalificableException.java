package org.mgroko.backend.proyectos.exception;

/**
 * Solo se pueden registrar calificaciones entre participantes cuando el
 * proyecto está finalizado.
 */
public class ProyectoNoCalificableException extends RuntimeException {

    public ProyectoNoCalificableException(String estadoActual) {
        super("Solo se puede calificar a los integrantes de un proyecto Finalizado. "
                + "Estado actual: " + estadoActual + ".");
    }
}
