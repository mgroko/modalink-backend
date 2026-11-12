package org.mgroko.backend.admin.dto;

import org.mgroko.backend.modelo.enums.EstadoPublicacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Acción administrativa de moderación sobre una publicación.
 *
 * <p>El motivo es obligatorio al ocultar y opcional al reactivar. Se registra
 * únicamente en auditoría; no se persiste en la publicación.</p>
 */
public record ModerarPublicacionRequest(
        @NotNull(message = "El estado es obligatorio.")
        EstadoPublicacion estado,

        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivo
) {
}
