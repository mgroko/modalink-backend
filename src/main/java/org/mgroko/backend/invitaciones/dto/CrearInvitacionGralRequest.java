package org.mgroko.backend.invitaciones.dto;

import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-31: invitación general al proyecto. No selecciona requerimiento.
 */
public record CrearInvitacionGralRequest(
        @NotNull(message = "El perfil destinatario es obligatorio.")
        Long idPerfilDestinatario,

        Long idRequerimientoGral,

        @Size(max = 200, message = "El mensaje no puede superar los 200 caracteres.")
        String mensaje,

        /**
         * Aceptación de los T&C vigentes del perfil destinatario. Es
         * obligatoria cuando el perfil tiene términos configurados.
         */
        @Valid
        AceptacionTerminosRequest aceptacionTerminos
) {
    public CrearInvitacionGralRequest(Long idPerfilDestinatario, String mensaje) {
        this(idPerfilDestinatario, null, mensaje, null);
    }

    public CrearInvitacionGralRequest(
            Long idPerfilDestinatario,
            Long idRequerimientoGral,
            String mensaje) {
        this(idPerfilDestinatario, idRequerimientoGral, mensaje, null);
    }
}
