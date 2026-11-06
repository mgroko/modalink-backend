package org.mgroko.backend.actividades.exception;

import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;

/**
 * El miembro no tiene un hueco suficiente dentro del rango factible de la
 * actividad. Conserva el resultado de disponibilidad para que el handler
 * devuelva el diagnóstico al frontend.
 */
public class AsignacionNoDisponibleException extends RuntimeException {

    private final ResultadoDisponibilidad disponibilidad;

    public AsignacionNoDisponibleException(ResultadoDisponibilidad disponibilidad) {
        super("El miembro no tiene disponibilidad suficiente dentro del rango factible de la actividad.");
        this.disponibilidad = disponibilidad;
    }

    public ResultadoDisponibilidad getDisponibilidad() {
        return disponibilidad;
    }
}
