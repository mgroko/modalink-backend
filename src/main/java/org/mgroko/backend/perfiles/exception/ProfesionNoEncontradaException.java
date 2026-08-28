package org.mgroko.backend.perfiles.exception;

public class ProfesionNoEncontradaException extends RuntimeException {
    public ProfesionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}