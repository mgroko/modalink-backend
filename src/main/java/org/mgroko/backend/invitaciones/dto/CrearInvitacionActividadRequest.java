package org.mgroko.backend.invitaciones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-31: invitación a un requerimiento de actividad del proyecto.
 */
public record CrearInvitacionActividadRequest(
        @NotNull(message = "El requerimiento de actividad es obligatorio.")
        Long idRequerimientoActividad,

        @NotNull(message = "El perfil destinatario es obligatorio.")
        Long idPerfilDestinatario,

        @Size(max = 200, message = "El mensaje no puede superar los 200 caracteres.")
        String mensaje
) {
}
