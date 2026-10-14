package org.mgroko.backend.proyectos.exception;

public class CriterioBusquedaInvalidoException extends RuntimeException {
    public CriterioBusquedaInvalidoException() {
        super("Debes indicar al menos un criterio de búsqueda: nombre, estado o director.");
    }
}
