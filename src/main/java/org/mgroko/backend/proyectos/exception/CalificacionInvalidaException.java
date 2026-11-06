package org.mgroko.backend.proyectos.exception;

/**
 * Calificación inválida: valor fuera de rango, reseña vacía, perfil ajeno al
 * proyecto, auto-calificación o duplicado en la misma solicitud.
 */
public class CalificacionInvalidaException extends RuntimeException {

    public CalificacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
