package org.mgroko.backend.reportes.exception;

/**
 * La publicación reportada pertenece al mismo usuario que el autor.
 */
public class AutoreportePublicacionException extends RuntimeException {

    public AutoreportePublicacionException() {
        super("No podés reportar una publicación propia.");
    }
}
