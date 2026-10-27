package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;

/**
 * Rango factible de una actividad ya proyectado a fechas absolutas.
 *
 * <p>Es la traducción de {@link RangoFactible} usando el {@code t0} del
 * proyecto. Para una actividad que conserva una fecha fija, el rango se
 * reduce a su ventana realmente agendada ({@code [inicio, fin]}); para una
 * actividad pendiente, el rango es {@code [ES, LF]}.</p>
 *
 * <p>Si el proyecto todavía no tiene fecha de inicio confirmada, el rango se
 * amplía a {@code [ahora, fecha de entrega]} y {@code tentativa} queda en
 * {@code true}: la actividad no está agendada, así que la verificación de
 * disponibilidad es provisional y debe repetirse en UC-60/UC-70.</p>
 *
 * @param inicio    inicio absoluto de la ventana
 * @param fin       fin absoluto de la ventana
 * @param tentativa {@code true} si el rango se proyectó desde una fecha
 *                  estimada y la verificación es provisional
 */
public record RangoFactibleAbsoluto(
        LocalDateTime fechaInicioMinima,
        LocalDateTime fechaInicioMaxima,
        LocalDateTime fechaFinMinima,
        LocalDateTime fechaFinMaxima,
        boolean fijo,
        boolean tentativa,
        boolean parcial,
        String motivoNoFactible) {

    public RangoFactibleAbsoluto {
        if (fechaInicioMinima == null || fechaInicioMaxima == null
                || fechaFinMinima == null || fechaFinMaxima == null) {
            throw new IllegalArgumentException("El rango factible no puede tener extremos nulos.");
        }
        if (fechaInicioMaxima.isBefore(fechaInicioMinima)
                || fechaFinMaxima.isBefore(fechaFinMinima)) {
            throw new IllegalArgumentException("El rango factible no puede tener extremos invertidos.");
        }
    }

    /** Compatibilidad para consumidores que aún evalúan la ventana completa. */
    public LocalDateTime inicio() {
        return fechaInicioMinima;
    }

    /** Compatibilidad para consumidores que aún evalúan la ventana completa. */
    public LocalDateTime fin() {
        return fechaFinMaxima;
    }
}
