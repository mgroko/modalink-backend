package org.mgroko.backend.contratos.exception;

public class TipoAcuerdoContratoNoEncontradoException extends RuntimeException {

    public TipoAcuerdoContratoNoEncontradoException(Long idTipoAcuerdo) {
        super("El tipo de acuerdo " + idTipoAcuerdo + " no existe o no esta activo.");
    }
}
