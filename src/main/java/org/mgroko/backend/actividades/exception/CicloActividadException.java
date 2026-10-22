package org.mgroko.backend.actividades.exception;

public class CicloActividadException extends RuntimeException {

    public CicloActividadException(String mensaje) {
        super(mensaje);
    }

    public CicloActividadException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
