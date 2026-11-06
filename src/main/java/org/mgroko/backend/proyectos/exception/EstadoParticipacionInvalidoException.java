package org.mgroko.backend.proyectos.exception;

public class EstadoParticipacionInvalidoException extends RuntimeException {

    public EstadoParticipacionInvalidoException(String estado) {
        super("El miembro ya no tiene una participación activa en el proyecto. Estado actual: "
                + estado + ".");
    }
}
