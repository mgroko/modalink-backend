package org.mgroko.backend.actividades.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * UC-70: solicitud de asignación directa de una actividad a un miembro del
 * proyecto.
 *
 * @param idPerfil perfil activo del miembro que se quiere asignar
 */
public record AsignarActividadRequest(
        @NotNull(message = "El perfil es obligatorio.")
        @Positive(message = "El perfil debe ser válido.")
        Long idPerfil
) {
}
