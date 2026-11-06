package org.mgroko.backend.actividades.evento;

/**
 * Evento de dominio que se publica cuando una actividad pasa a CANCELADA.
 *
 * <p>Se publica siempre, aunque la actividad no tenga asignaciones activas.
 * El módulo de reemplazos lo usa para cancelar lógicamente los procesos e
 * invitaciones asociados.</p>
 */
public record ActividadCanceladaEvent(
        Long idActividad,
        Long idProyecto,
        String nombreActividad,
        String nombreProyecto) {
}
