package org.mgroko.backend.invitaciones.dto;

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
        String mensaje
) {
    public CrearInvitacionGralRequest(Long idPerfilDestinatario, String mensaje) {
        this(idPerfilDestinatario, null, mensaje);
    }
}
