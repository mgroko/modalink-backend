package org.mgroko.backend.invitaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

public record InvitacionResponse(
        Long idInvitacion,
        TipoInvitacion tipo,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        Long idProyecto,
        Long idRequerimientoGral,
        Long idRequerimientoActividad,
        Long idPerfilRemitente,
        Long idPerfilDestinatario,
        String mensaje
) {
}
