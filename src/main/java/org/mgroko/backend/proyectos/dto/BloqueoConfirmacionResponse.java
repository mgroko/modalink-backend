package org.mgroko.backend.proyectos.dto;

/**
 * Bloqueo o inconsistencia que impide confirmar un proyecto.
 *
 * @param codigo      código estable para el frontend
 * @param mensaje     descripción legible
 * @param entidadTipo tipo de entidad afectada ({@code PROYECTO},
 *                    {@code ACTIVIDAD}, {@code REQUERIMIENTO_GRAL},
 *                    {@code REQUERIMIENTO_ACTIVIDAD}, {@code CRONOGRAMA})
 * @param entidadId   identificador de la entidad afectada, si aplica
 */
public record BloqueoConfirmacionResponse(
        String codigo,
        String mensaje,
        String entidadTipo,
        Long entidadId) {
}
