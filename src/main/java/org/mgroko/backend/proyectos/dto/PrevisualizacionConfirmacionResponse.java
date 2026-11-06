package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de la previsualización de UC-60.
 */
public record PrevisualizacionConfirmacionResponse(
        boolean puedeConfirmar,
        String estado,
        LocalDateTime fechaInicioPropuesta,
        LocalDate fechaEntrega,
        List<BloqueoConfirmacionResponse> bloqueos,
        List<RequerimientoCupoResponse> requerimientosGenerales,
        List<RequerimientoCupoResponse> requerimientosActividad,
        List<ActividadProgramadaResponse> propuestaHorarios) {

    public PrevisualizacionConfirmacionResponse {
        bloqueos = bloqueos == null ? List.of() : List.copyOf(bloqueos);
        requerimientosGenerales = requerimientosGenerales == null
                ? List.of()
                : List.copyOf(requerimientosGenerales);
        requerimientosActividad = requerimientosActividad == null
                ? List.of()
                : List.copyOf(requerimientosActividad);
        propuestaHorarios = propuestaHorarios == null
                ? List.of()
                : List.copyOf(propuestaHorarios);
    }
}
