package org.mgroko.backend.polaroids.exception;

public class PolaroidNoEncontradaException extends RuntimeException {

    public PolaroidNoEncontradaException(Long idPolaroid) {
        super("Polaroid no encontrada: " + idPolaroid);
    }
}
