package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de verificar la disponibilidad de un usuario frente al rango
 * factible de una actividad.
 *
 * @param disponible  {@code true} si existe un hueco libre continuo de al
 *                    menos la duración de la actividad dentro del rango
 * @param provisional {@code true} si el rango se proyectó desde una fecha de
 *                    inicio estimada; en ese caso la verificación debe
 *                    repetirse al confirmar el proyecto (UC-60) o al asignar
 *                    la actividad (UC-70)
 * @param inicioEvaluado      inicio absoluto del rango evaluado
 * @param finEvaluado         fin absoluto del rango evaluado
 * @param inicioSeleccionado  inicio del primer hueco común
 * @param finSeleccionado     fin del primer hueco común
 * @param motivo              explicación cuando no hay disponibilidad
 * @param miembros            detalle individual de cada miembro evaluado
 */
public record ResultadoDisponibilidad(
        boolean disponible,
        boolean provisional,
        LocalDateTime inicioEvaluado,
        LocalDateTime finEvaluado,
        LocalDateTime inicioSeleccionado,
        LocalDateTime finSeleccionado,
        String motivo,
        List<DetalleMiembroDisponibilidad> miembros) {

    public ResultadoDisponibilidad(
            boolean disponible,
            boolean provisional,
            LocalDateTime inicio,
            LocalDateTime fin) {
        this(disponible, provisional, inicio, fin, null, null, null, List.of());
    }

    /**
     * Accesores históricos: representan el primer hueco seleccionado cuando
     * existe y, si no, el rango evaluado.
     */
    public LocalDateTime inicio() {
        return inicioSeleccionado != null ? inicioSeleccionado : inicioEvaluado;
    }

    public LocalDateTime fin() {
        return finSeleccionado != null ? finSeleccionado : finEvaluado;
    }

    public ResultadoDisponibilidad {
        miembros = miembros == null ? List.of() : List.copyOf(miembros);
    }
}
