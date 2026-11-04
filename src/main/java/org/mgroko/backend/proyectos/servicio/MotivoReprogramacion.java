package org.mgroko.backend.proyectos.servicio;

/**
 * Origen de una reprogramación de proyecto publicado o confirmado.
 */
public enum MotivoReprogramacion {
    CAMBIO_ENTREGA,
    CAMBIO_JORNADA,
    CAMBIO_MARGEN,
    CAMBIO_BLOQUEO,
    CREACION_ACTIVIDAD,
    CANCELACION_ACTIVIDAD,
    ASIGNACION,
    DESASIGNACION,
    BAJA_INTEGRANTE,
    ELIMINACION_INTEGRANTE,
    ELIMINACION_ACTIVIDAD,
    REPARACION,
    REINTENTO
}
