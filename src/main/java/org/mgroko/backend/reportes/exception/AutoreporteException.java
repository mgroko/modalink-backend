package org.mgroko.backend.reportes.exception;

/**
 * El perfil reportado pertenece al mismo usuario que el autor.
 */
public class AutoreporteException extends RuntimeException {

    public AutoreporteException() {
        super("No podés reportar un perfil propio.");
    }
}
