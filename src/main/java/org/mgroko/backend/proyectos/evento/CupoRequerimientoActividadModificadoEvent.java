package org.mgroko.backend.proyectos.evento;

/**
 * Se publica cuando una operación puede haber cambiado la cobertura de un
 * requerimiento de actividad (asignaciones, aceptaciones de postulaciones o
 * invitaciones, aprobaciones de reemplazo o cambios de cantidad).
 *
 * <p>El módulo de reemplazos lo consume dentro de la misma transacción para
 * cancelar los procesos que quedaron sin vacante.</p>
 */
public record CupoRequerimientoActividadModificadoEvent(Long idRequerimiento) {
}
