package org.mgroko.backend.admin.exception;

public class ProcesamientoHistoricoNoCancelableException extends RuntimeException {

    public ProcesamientoHistoricoNoCancelableException() {
        super("El procesamiento ya no se puede cancelar.");
    }
}
