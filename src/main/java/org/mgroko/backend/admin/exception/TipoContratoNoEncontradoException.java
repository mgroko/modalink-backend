package org.mgroko.backend.admin.exception;

public class TipoContratoNoEncontradoException extends RuntimeException {

    public TipoContratoNoEncontradoException(Long idTipoAcuerdo) {
        super("El tipo de contrato " + idTipoAcuerdo + " no existe.");
    }
}
