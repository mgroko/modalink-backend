package org.mgroko.backend.proyectos.exception;

public class UltimoDirectorActivoException extends RuntimeException {

    public UltimoDirectorActivoException() {
        super("El proyecto debe conservar al menos un director activo.");
    }
}
