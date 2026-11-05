package org.mgroko.backend.proyectos.exception;

/**
 * Parámetros inválidos para la consulta del historial de membresías.
 */
public class HistorialParametroInvalidoException extends RuntimeException {

    public HistorialParametroInvalidoException(String message) {
        super(message);
    }
}
