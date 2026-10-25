package org.mgroko.backend.actividades.exception;

public class NombreActividadDuplicadoException extends RuntimeException {

    public NombreActividadDuplicadoException(String nombre) {
        super("Ya existe una actividad con el nombre '" + nombre + "' en esta planificacion.");
    }
}
