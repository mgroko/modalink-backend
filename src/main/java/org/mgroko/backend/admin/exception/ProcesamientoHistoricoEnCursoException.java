package org.mgroko.backend.admin.exception;

public class ProcesamientoHistoricoEnCursoException extends RuntimeException {

    public ProcesamientoHistoricoEnCursoException() {
        super("Ya hay un procesamiento histórico en curso.");
    }
}
