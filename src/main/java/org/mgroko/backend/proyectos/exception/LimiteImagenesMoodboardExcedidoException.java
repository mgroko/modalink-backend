package org.mgroko.backend.proyectos.exception;

public class LimiteImagenesMoodboardExcedidoException extends RuntimeException {
    public LimiteImagenesMoodboardExcedidoException(int limite) {
        super("El moodboard no puede contener más de " + limite + " imágenes.");
    }
}
