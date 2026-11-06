package org.mgroko.backend.reemplazos.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;

public record ReemplazoResumenResponse(
        Long idReemplazo,
        TipoReemplazo tipo,
        Long idProyecto,
        String nombreProyecto,
        Long idActividad,
        String nombreActividad,
        Long idRequerimientoActividad,
        Long idRequerimientoGeneral,
        String profesion,
        String perfilBaja,
        OrigenReemplazo origen,
        EstadoReemplazo estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        String motivoCierre,
        int totalCandidatos,
        int totalInvitados) {
}
