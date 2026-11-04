package org.mgroko.backend.modelo.enums;

/**
 * Origen efectivo del inicio de una actividad.
 *
 * <p>Es un valor derivado, no persistido: se calcula a partir del estado de la
 * actividad, su {@code fechaHoraInicio}, las asignaciones activas y el estado
 * del proyecto. No debe usarse para decidir movilidad; solo
 * {@code EN_CURSO} y {@code FINALIZADA} son inmóviles.</p>
 */
public enum OrigenInicio {
    PROYECTADA,
    CONFIRMADA,
    REAL
}
