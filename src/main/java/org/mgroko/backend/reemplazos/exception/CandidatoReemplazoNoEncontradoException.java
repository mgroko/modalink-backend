package org.mgroko.backend.reemplazos.exception;

public class CandidatoReemplazoNoEncontradoException extends RuntimeException {

    public CandidatoReemplazoNoEncontradoException(Long idCandidato) {
        super("El candidato de reemplazo " + idCandidato + " no existe.");
    }
}
