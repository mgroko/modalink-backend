package org.mgroko.backend.invitaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Ítem de UC-30. Unifica invitaciones generales y de actividad.
 */
public record InvitacionBusquedaResponse(
        Long idInvitacion,
        TipoInvitacion tipo,
        DireccionInvitacion direccion,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        Long idProyecto,
        String nombreProyecto,
        Long idRequerimientoGral,
        Long idRequerimientoActividad,
        Long idActividad,
        String nombreActividad,
        PerfilInvitacionResponse remitente,
        PerfilInvitacionResponse destinatario,
        String mensaje,
        boolean puedeEliminar
) {
}
