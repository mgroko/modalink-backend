package org.mgroko.backend.polaroids.exception;

public class TipoPolaroidNoEncontradoException extends RuntimeException {

    public TipoPolaroidNoEncontradoException(Long idTipoPolaroid) {
        super("Tipo de polaroid no encontrado: " + idTipoPolaroid);
    }
}
