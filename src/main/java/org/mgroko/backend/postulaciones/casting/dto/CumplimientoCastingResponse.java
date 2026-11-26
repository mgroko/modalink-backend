package org.mgroko.backend.postulaciones.casting.dto;

import java.math.BigDecimal;
import java.util.List;

public record CumplimientoCastingResponse(
        boolean cumpleTodos,
        int requisitosCumplidos,
        int requisitosEvaluados,
        BigDecimal porcentaje,
        List<String> faltantes,
        List<CaracteristicaEvaluadaCastingResponse> caracteristicas,
        List<HabilidadEvaluadaCastingResponse> habilidades,
        AgendaCastingResponse agenda) {

    public CumplimientoCastingResponse {
        faltantes = faltantes == null ? List.of() : List.copyOf(faltantes);
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        habilidades = habilidades == null ? List.of() : List.copyOf(habilidades);
    }
}
