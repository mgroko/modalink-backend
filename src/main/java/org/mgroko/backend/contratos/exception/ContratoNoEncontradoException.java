package org.mgroko.backend.contratos.exception;

public class ContratoNoEncontradoException extends RuntimeException {

    public ContratoNoEncontradoException(Long idContrato) {
        super("El contrato " + idContrato + " no existe.");
    }
}
