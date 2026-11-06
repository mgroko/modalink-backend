package org.mgroko.backend.actividades.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-51: eliminación de una actividad con las decisiones sobre sus miembros
 * exclusivos.
 */
public record EliminarActividadRequest(
        @NotBlank(message = "El motivo de eliminación es obligatorio.")
        @Size(max = 200, message = "El motivo de eliminación no puede superar los 200 caracteres.")
        String motivo,

        @NotNull(message = "La lista de decisiones es obligatoria.")
        List<@Valid DecisionIntegranteRequest> decisiones
) {
}
