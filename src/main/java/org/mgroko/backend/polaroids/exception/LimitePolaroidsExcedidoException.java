package org.mgroko.backend.polaroids.exception;

public class LimitePolaroidsExcedidoException extends RuntimeException {

    public LimitePolaroidsExcedidoException(int maximo) {
        super("Ya alcanzaste el máximo de " + maximo + " polaroids.");
    }
}
