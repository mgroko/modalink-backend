package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Resultado de la finalización de un proyecto (UC-37).
 */
public record FinalizarProyectoResponse(
        ProyectoResponse proyecto,
        LocalDateTime fechaFinalizacion,
        int calificacionesRegistradas) {
}
