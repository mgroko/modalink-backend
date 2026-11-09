package org.mgroko.backend.reemplazos.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;

public record InvitacionReemplazoResponse(
        Long idCandidato,
        Long idReemplazo,
        TipoReemplazo tipo,
        EstadoCandidatoReemplazo estado,
        Long idProyecto,
        String nombreProyecto,
        Long idActividad,
        String nombreActividad,
        String profesion,
        LocalDateTime fechaInvitacion,
        LocalDateTime fechaHoraInicio,
        Integer duracionMinutos,
        String mensaje,
        LocalDateTime fechaRespuesta,
        String motivoRespuesta) {
}
