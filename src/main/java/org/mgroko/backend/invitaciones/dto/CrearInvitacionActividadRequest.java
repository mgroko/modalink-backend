package org.mgroko.backend.invitaciones.dto;

import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;

import jakarta.validation.Valid;
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
        String mensaje,

        /**
         * Aceptación de los T&C vigentes del perfil destinatario. Es
         * obligatoria cuando el perfil tiene términos configurados.
         */
        @Valid
        AceptacionTerminosRequest aceptacionTerminos
) {
    public CrearInvitacionActividadRequest(
            Long idRequerimientoActividad,
            Long idPerfilDestinatario,
            String mensaje) {
        this(idRequerimientoActividad, idPerfilDestinatario, mensaje, null);
    }
}
