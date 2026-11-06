package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Actividad cuyo inicio cambió durante una reprogramación.
 */
public record ActividadReprogramadaResponse(
        Long idActividad,
        String nombre,
        String estado,
        LocalDateTime inicioAnterior,
        LocalDateTime inicioNuevo) {
}
