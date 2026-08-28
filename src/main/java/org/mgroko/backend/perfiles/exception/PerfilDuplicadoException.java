package org.mgroko.backend.perfiles.exception;

public class PerfilDuplicadoException extends RuntimeException {
    public PerfilDuplicadoException(String mensaje) {
        super(mensaje);
    }
}