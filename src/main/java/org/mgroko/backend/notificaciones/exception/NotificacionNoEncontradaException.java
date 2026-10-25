package org.mgroko.backend.notificaciones.exception;

public class NotificacionNoEncontradaException extends RuntimeException {

    public NotificacionNoEncontradaException() {
        super("La notificación no fue encontrada.");
    }
}
