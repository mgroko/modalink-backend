package org.mgroko.backend.perfiles.exception;

public class CaracteristicaValorNoCoincideException extends RuntimeException {
    public CaracteristicaValorNoCoincideException(String mensaje) {
        super(mensaje);
    }
}