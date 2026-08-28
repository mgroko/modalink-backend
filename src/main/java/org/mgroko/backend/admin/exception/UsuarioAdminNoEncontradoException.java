package org.mgroko.backend.admin.exception;

public class UsuarioAdminNoEncontradoException extends RuntimeException {
    public UsuarioAdminNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}