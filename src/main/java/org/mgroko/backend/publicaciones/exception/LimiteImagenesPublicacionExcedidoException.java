package org.mgroko.backend.publicaciones.exception;

public class LimiteImagenesPublicacionExcedidoException extends RuntimeException {

    public LimiteImagenesPublicacionExcedidoException(int limite) {
        super("Una publicación puede tener como máximo " + limite + " imágenes.");
    }
}
