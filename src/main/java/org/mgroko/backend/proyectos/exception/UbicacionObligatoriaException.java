package org.mgroko.backend.proyectos.exception;

public class UbicacionObligatoriaException extends RuntimeException {

    public UbicacionObligatoriaException() {
        super("La ubicación es obligatoria si el proyecto está en estado publicado o confirmado.");
    }
}
