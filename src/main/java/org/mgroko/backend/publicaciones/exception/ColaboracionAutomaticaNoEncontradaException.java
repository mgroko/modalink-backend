package org.mgroko.backend.publicaciones.exception;

public class ColaboracionAutomaticaNoEncontradaException extends RuntimeException {

    public ColaboracionAutomaticaNoEncontradaException() {
        super("No se encontró la colaboración automática indicada.");
    }
}
