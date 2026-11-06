package org.mgroko.backend.proyectos.dto;

/**
 * Bloqueo o inconsistencia que impide finalizar un proyecto (UC-37).
 *
 * @param codigo      código estable para el frontend
 * @param mensaje     descripción legible
 * @param entidadTipo tipo de entidad afectada ({@code PROYECTO},
 *                    {@code ACTIVIDAD}, {@code PLANIFICACION})
 * @param entidadId   identificador de la entidad afectada, si aplica
 */
public record BloqueoFinalizacionResponse(
        String codigo,
        String mensaje,
        String entidadTipo,
        Long entidadId) {
}
