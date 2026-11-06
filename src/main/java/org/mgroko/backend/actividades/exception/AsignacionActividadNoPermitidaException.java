package org.mgroko.backend.actividades.exception;

/**
 * Conflicto de negocio al intentar asignar una actividad a un miembro:
 * duplicado, membresía inactiva, estado de proyecto/actividad no permitido o
 * ausencia de agenda.
 */
public class AsignacionActividadNoPermitidaException extends RuntimeException {

    public AsignacionActividadNoPermitidaException(String message) {
        super(message);
    }
}
