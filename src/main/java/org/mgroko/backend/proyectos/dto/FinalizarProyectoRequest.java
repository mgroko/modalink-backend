package org.mgroko.backend.proyectos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud de finalización de un proyecto. Incluye las calificaciones que el
 * director debe registrar para todos los miembros activos del equipo.
 */
public record FinalizarProyectoRequest(
        @NotNull(message = "La lista de calificaciones es obligatoria.")
        List<@Valid CalificacionRequest> calificaciones) {
}
