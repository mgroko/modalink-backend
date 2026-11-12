package org.mgroko.backend.actividades.exception;

/**
 * Se lanza cuando se intenta eliminar una actividad que tiene procesos de
 * reemplazo asociados. El historial de reemplazos referencia la actividad y
 * debe conservarse.
 */
public class ActividadConReemplazosException extends RuntimeException {

    public ActividadConReemplazosException(Long idActividad) {
        super("No se puede eliminar la actividad " + idActividad
                + " porque tiene reemplazos asociados. "
                + "El historial de reemplazos debe conservarse.");
    }
}
