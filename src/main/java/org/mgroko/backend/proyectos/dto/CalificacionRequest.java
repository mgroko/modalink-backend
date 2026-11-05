package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Calificación y reseña de un miembro del proyecto.
 */
public record CalificacionRequest(
        @NotNull(message = "El perfil a calificar es obligatorio.")
        Long idPerfil,

        @NotNull(message = "La valoración es obligatoria.")
        @Min(value = 1, message = "La valoración mínima es 1.")
        @Max(value = 5, message = "La valoración máxima es 5.")
        Integer valoracion,

        @NotBlank(message = "La reseña es obligatoria.")
        @Size(max = 200, message = "La reseña no puede superar los 200 caracteres.")
        String resenia) {
}
