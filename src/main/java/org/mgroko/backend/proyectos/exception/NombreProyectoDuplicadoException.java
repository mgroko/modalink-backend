package org.mgroko.backend.proyectos.exception;

public class NombreProyectoDuplicadoException extends RuntimeException {
    public NombreProyectoDuplicadoException(String nombre) {
        super("Ya tienes un proyecto con el nombre '" + nombre + "'.");
    }
}
