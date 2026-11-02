package org.mgroko.backend.actividades.exception;

/**
 * La asignación indicada no existe o no pertenece a la actividad/proyecto del
 * endpoint.
 */
public class AsignacionActividadNoEncontradaException extends RuntimeException {

    public AsignacionActividadNoEncontradaException(Long idAsignacion) {
        super("La asignación " + idAsignacion
                + " no existe o no pertenece a la actividad y proyecto indicados.");
    }
}
