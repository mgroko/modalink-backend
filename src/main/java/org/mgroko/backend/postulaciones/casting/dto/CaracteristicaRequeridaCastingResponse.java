package org.mgroko.backend.postulaciones.casting.dto;

import java.math.BigDecimal;
import java.util.List;

public record CaracteristicaRequeridaCastingResponse(
        Long idCaracteristica,
        String codigo,
        String nombre,
        String tipoDato,
        BigDecimal valorMin,
        BigDecimal valorMax,
        String unidad,
        List<ValorRequeridoCastingResponse> valores) {

    public CaracteristicaRequeridaCastingResponse {
        valores = valores == null ? List.of() : List.copyOf(valores);
    }
}
