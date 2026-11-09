package org.mgroko.backend.invitaciones.dto;

import java.util.List;

import org.mgroko.backend.perfiles.dto.AceptacionTerminosPorPerfilRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-31: invitación general a varios perfiles en una sola operación.
 */
public record CrearInvitacionGralBatchRequest(
        @NotEmpty(message = "Seleccioná al menos un perfil destinatario.")
        List<@NotNull(message = "El perfil destinatario no puede ser nulo.")
                Long> idsPerfilDestinatario,

        Long idRequerimientoGral,

        @Size(max = 200, message = "El mensaje no puede superar los 200 caracteres.")
        String mensaje,

        /**
         * Aceptaciones de los T&C vigentes de cada perfil destinatario. Solo
         * se exigen para los perfiles que tienen términos configurados.
         */
        List<@Valid AceptacionTerminosPorPerfilRequest> aceptacionesTerminos
) {
    public CrearInvitacionGralBatchRequest(
            List<Long> idsPerfilDestinatario, String mensaje) {
        this(idsPerfilDestinatario, null, mensaje, null);
    }

    public CrearInvitacionGralBatchRequest(
            List<Long> idsPerfilDestinatario,
            Long idRequerimientoGral,
            String mensaje) {
        this(idsPerfilDestinatario, idRequerimientoGral, mensaje, null);
    }
}
