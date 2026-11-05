package org.mgroko.backend.proyectos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Calificaciones que un participante activo registra sobre sus compañeros
 * después de que el proyecto fue finalizado (UC-37).
 */
public record CalificarEquipoRequest(
        @NotNull(message = "La lista de calificaciones es obligatoria.")
        List<@Valid CalificacionRequest> calificaciones) {
}
