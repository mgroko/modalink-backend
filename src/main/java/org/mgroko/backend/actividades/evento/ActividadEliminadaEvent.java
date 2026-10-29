package org.mgroko.backend.actividades.evento;

import java.util.List;

public record ActividadEliminadaEvent(
        Long idActividad,
        Long idProyecto,
        String nombreActividad,
        String nombreProyecto,
        String motivo,
        List<Long> idsPerfilesAsignados,
        List<PostulacionAfectada> postulaciones,
        List<InvitacionAfectada> invitaciones) {

    public record PostulacionAfectada(Long idPostulacion, Long idPerfil) {
    }

    public record InvitacionAfectada(Long idInvitacion, Long idPerfil) {
    }
}
