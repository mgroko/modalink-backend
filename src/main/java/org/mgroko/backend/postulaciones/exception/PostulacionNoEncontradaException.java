package org.mgroko.backend.postulaciones.exception;

public class PostulacionNoEncontradaException extends RuntimeException {

    public PostulacionNoEncontradaException() {
        super("La postulación no existe o no pertenece al proyecto indicado.");
    }
}
