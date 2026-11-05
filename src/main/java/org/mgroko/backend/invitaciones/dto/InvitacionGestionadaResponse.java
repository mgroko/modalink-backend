package org.mgroko.backend.invitaciones.dto;

import java.util.List;

import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.dto.AdvertenciaSobrecompromiso;

/**
 * Resultado de aceptar o rechazar una invitación (UC-28).
 */
public record InvitacionGestionadaResponse(
        Long idInvitacion,
        TipoInvitacion tipo,
        EstadoSolicitud estado,
        Long idProyecto,
        Long idPerfil,
        Long idMiembro,
        Long idAsignacionActividad,
        ResultadoDisponibilidad disponibilidad,
        List<AdvertenciaSobrecompromiso> advertencias
) {
    public InvitacionGestionadaResponse {
        advertencias = advertencias == null ? List.of() : List.copyOf(advertencias);
    }
}
