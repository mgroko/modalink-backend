package org.mgroko.backend.perfiles.exception;

public class PerfilNoEncontradoException extends RuntimeException {
    public PerfilNoEncontradoException() {
        super("El perfil no fue encontrado");
    }
}