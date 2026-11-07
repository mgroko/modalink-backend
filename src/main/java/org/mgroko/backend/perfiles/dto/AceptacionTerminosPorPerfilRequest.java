package org.mgroko.backend.perfiles.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aceptación de los términos de un perfil dentro de una operación masiva
 * (invitaciones batch de UC-31).
 */
public record AceptacionTerminosPorPerfilRequest(
        @NotNull(message = "El perfil es obligatorio.")
        Long idPerfil,

        Long idTyc,

        Boolean aceptaTerminos
) {
}
