package org.mgroko.backend.admin.exception;

public class UsuarioEnBajaException extends RuntimeException {
    public UsuarioEnBajaException(String mensaje) {
        super(mensaje);
    }
}