package org.mgroko.backend.calendario.dto;

/**
 * Resultado de aplicar el horario de preferencia de una actividad sobre el
 * hueco seleccionado.
 *
 * <p>La preferencia es una restricción blanda: si no se puede cumplir
 * exactamente, se admite un desvío dentro de la tolerancia configurada. Nunca
 * se relajan la entrega, la jornada, los bloqueos ni las precedencias.</p>
 */
public enum ResultadoPreferencia {

    /** La actividad no tiene horario de preferencia. */
    SIN_PREFERENCIA,

    /** El inicio seleccionado coincide con la hora preferida. */
    EXACTO,

    /** El inicio seleccionado se adelanta o atrasa dentro de la tolerancia. */
    DESVIO,

    /** Existe hueco, pero fuera de la tolerancia, o no existe hueco. */
    SIN_LUGAR
}
