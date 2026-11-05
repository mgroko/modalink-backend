package org.mgroko.backend.invitaciones.mapper;

import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.RequerimientoActividad;

public final class InvitacionMapper {

    private InvitacionMapper() {
    }

    public static InvitacionResponse toResponse(InvitacionGral invitacion) {
        return new InvitacionResponse(
                invitacion.getIdInvitacionGral(),
                TipoInvitacion.GENERAL,
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                invitacion.getProyecto().getIdProyecto(),
                invitacion.getRequerimientoGral() != null
                        ? invitacion.getRequerimientoGral().getIdRequerimientoGral()
                        : null,
                null,
                invitacion.getPerfilRemitente().getIdPerfil(),
                invitacion.getPerfilDestinatario().getIdPerfil(),
                invitacion.getMensaje());
    }

    public static InvitacionResponse toResponse(InvitacionActividad invitacion) {
        RequerimientoActividad requerimiento = invitacion.getRequerimiento();
        Long idProyecto = requerimiento.getActividad()
                .getPlanificacion()
                .getProyecto()
                .getIdProyecto();

        return new InvitacionResponse(
                invitacion.getIdInvitacion(),
                TipoInvitacion.ACTIVIDAD,
                invitacion.getEstado(),
                invitacion.getFechaEnvio(),
                idProyecto,
                null,
                requerimiento.getIdRequerimiento(),
                invitacion.getPerfilRemitente().getIdPerfil(),
                invitacion.getPerfilDestinatario().getIdPerfil(),
                invitacion.getMensaje());
    }
}
