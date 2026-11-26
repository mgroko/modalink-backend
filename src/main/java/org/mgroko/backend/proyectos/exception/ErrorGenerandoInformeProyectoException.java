package org.mgroko.backend.proyectos.exception;

/**
 * Error al generar el PDF del informe de proyecto.
 */
public class ErrorGenerandoInformeProyectoException extends RuntimeException {

    public ErrorGenerandoInformeProyectoException(String message, Throwable cause) {
        super(message, cause);
    }
}
