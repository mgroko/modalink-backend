package org.mgroko.backend.proyectos.exception;

public class EstadoProyectoNoPermiteBajaVoluntariaException extends RuntimeException {

    public EstadoProyectoNoPermiteBajaVoluntariaException(String estadoActual) {
        super("Solo se puede dar de baja en un proyecto en estado Publicado o Confirmado. Estado actual: "
                + estadoActual + ".");
    }
}
