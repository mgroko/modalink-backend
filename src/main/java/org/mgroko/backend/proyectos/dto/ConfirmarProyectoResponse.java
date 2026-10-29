package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.actividades.dto.CronogramaResponse;

/**
 * Respuesta de UC-60: proyecto confirmado y cronograma resultante.
 */
public record ConfirmarProyectoResponse(
        ProyectoResponse proyecto,
        LocalDateTime fechaInicioConfirmada,
        CronogramaResponse cronograma) {
}
