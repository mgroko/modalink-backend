package org.mgroko.backend.perfiles.exception;

public class CaracteristicaNoEncontradaException extends RuntimeException {
    public CaracteristicaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}