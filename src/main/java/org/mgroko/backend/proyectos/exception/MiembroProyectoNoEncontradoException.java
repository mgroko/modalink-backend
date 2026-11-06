package org.mgroko.backend.proyectos.exception;

public class MiembroProyectoNoEncontradoException extends RuntimeException {

    public MiembroProyectoNoEncontradoException(Long idMiembro) {
        super("El miembro de proyecto con id " + idMiembro + " no existe.");
    }
}
