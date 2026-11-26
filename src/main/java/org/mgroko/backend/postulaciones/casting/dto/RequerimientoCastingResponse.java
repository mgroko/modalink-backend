package org.mgroko.backend.postulaciones.casting.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.postulaciones.dto.TipoPostulacion;

public record RequerimientoCastingResponse(
        TipoPostulacion tipo,
        Long idRequerimiento,
        Long idActividad,
        String nombreActividad,
        String descripcion,
        Long idProfesion,
        String codigoProfesion,
        String nombreProfesion,
        Integer cantidad,
        long cubiertos,
        int faltantes,
        boolean satisfecho,
        boolean castingCerrado,
        long postulacionesPendientes,
        boolean agendaRequerida,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        Integer duracionMinutos,
        List<CaracteristicaRequeridaCastingResponse> caracteristicas,
        List<HabilidadCastingResponse> habilidades) {

    public RequerimientoCastingResponse {
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        habilidades = habilidades == null ? List.of() : List.copyOf(habilidades);
    }
}
