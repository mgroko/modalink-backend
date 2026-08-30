package org.mgroko.backend.ubicacion.exception;

/**
 * Se lanza cuando el id de localidad enviado no existe en el catálogo de Georef.
 */
public class LocalidadNoEncontradaException extends RuntimeException {

    public LocalidadNoEncontradaException(String message) {
        super(message);
    }
}