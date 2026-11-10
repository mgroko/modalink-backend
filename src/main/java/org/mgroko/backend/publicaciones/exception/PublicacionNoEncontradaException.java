package org.mgroko.backend.publicaciones.exception;

public class PublicacionNoEncontradaException extends RuntimeException {

    public PublicacionNoEncontradaException(Long idPublicacion) {
        super("No se encontró la publicación indicada.");
    }

    public PublicacionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
