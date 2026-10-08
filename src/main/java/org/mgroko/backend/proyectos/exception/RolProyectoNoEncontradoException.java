package org.mgroko.backend.proyectos.exception;

public class RolProyectoNoEncontradoException extends RuntimeException {
    public RolProyectoNoEncontradoException(String rol) {
        super("No se encontró el rol de proyecto '" + rol + "'.");
    }
}
