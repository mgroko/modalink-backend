package org.mgroko.backend.modelo.enums;

/**
 * Procedencia del inicio de una actividad.
 *
 * <p>Permite distinguir una proyección de una fecha confirmada y de un inicio
 * real, sin depender solo de que {@code fechaHoraInicio} no sea nulo.</p>
 */
public enum OrigenInicio {
    PROYECTADA,
    CONFIRMADA,
    REAL
}
