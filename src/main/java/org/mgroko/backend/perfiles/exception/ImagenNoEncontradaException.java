package org.mgroko.backend.perfiles.exception;

public class ImagenNoEncontradaException extends RuntimeException {
    public ImagenNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}