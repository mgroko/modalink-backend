package org.mgroko.backend.proyectos.exception;

public class ProyectoNoEncontradoException extends RuntimeException {
    public ProyectoNoEncontradoException(Long idProyecto) {
        super("El proyecto con id " + idProyecto + " no existe.");
    }
}
