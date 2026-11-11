package org.mgroko.backend.publicaciones.exception;

public class SolicitudColaboracionNoEncontradaException extends RuntimeException {

    public SolicitudColaboracionNoEncontradaException() {
        super("La solicitud de colaboración no fue encontrada.");
    }
}
