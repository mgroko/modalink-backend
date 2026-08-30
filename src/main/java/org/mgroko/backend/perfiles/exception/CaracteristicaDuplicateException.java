package org.mgroko.backend.perfiles.exception;

public class CaracteristicaDuplicateException extends RuntimeException {
    public CaracteristicaDuplicateException(String mensaje) {
        super(mensaje);
    }
}