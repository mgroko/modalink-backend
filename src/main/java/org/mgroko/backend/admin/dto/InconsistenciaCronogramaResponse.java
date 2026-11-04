package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Inconsistencia detectada en el cronograma de un proyecto confirmado.
 */
public record InconsistenciaCronogramaResponse(
        Long idProyecto,
        String nombreProyecto,
        Long idActividad,
        String nombreActividad,
        String tipo,
        String detalle,
        LocalDateTime inicioActual,
        LocalDateTime finMaximoPredecesoras) {
}
