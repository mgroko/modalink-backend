package org.mgroko.backend.actividades.dto;

import org.mgroko.backend.modelo.enums.EstadoActividad;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoActividadRequest(
        @NotNull(message = "El estado es obligatorio.")
        EstadoActividad estado
) {
}
