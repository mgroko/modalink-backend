package org.mgroko.backend.home.dto;

import java.util.List;

import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;

public record HomeResumenResponse(
        PerfilResponse perfilActivo,
        List<Object> proyectosDestacados,
        List<Object> publicacionesRecientes,
        List<RequerimientoOportunidadResponse> oportunidades,
        long totalOportunidadesGenerales,
        long totalOportunidadesActividad) {

    public HomeResumenResponse {
        oportunidades = oportunidades == null ? List.of() : List.copyOf(oportunidades);
    }
}
