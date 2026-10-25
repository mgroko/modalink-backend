package org.mgroko.backend.actividades.exception;

public class FechaEntregaNoDefinidaException extends RuntimeException {

    public FechaEntregaNoDefinidaException() {
        super("El proyecto no tiene fecha de entrega definida. No es posible calcular el cronograma. Complete la fecha de entrega para continuar.");
    }
}
