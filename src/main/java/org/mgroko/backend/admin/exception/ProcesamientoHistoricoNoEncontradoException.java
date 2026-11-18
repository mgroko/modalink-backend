package org.mgroko.backend.admin.exception;

public class ProcesamientoHistoricoNoEncontradoException extends RuntimeException {

    public ProcesamientoHistoricoNoEncontradoException() {
        super("El procesamiento no existe o ya no está disponible.");
    }
}
