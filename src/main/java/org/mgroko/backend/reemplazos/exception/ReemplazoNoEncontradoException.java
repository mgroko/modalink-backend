package org.mgroko.backend.reemplazos.exception;

public class ReemplazoNoEncontradoException extends RuntimeException {

    public ReemplazoNoEncontradoException() {
        super("El proceso de reemplazo no existe.");
    }

    public ReemplazoNoEncontradoException(Long idReemplazo) {
        super("El proceso de reemplazo " + idReemplazo + " no existe.");
    }
}
