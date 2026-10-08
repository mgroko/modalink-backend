package org.mgroko.backend.proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

public record CaracteristicaRequerimientoResponse(
        Long idCaracteristica,
        String codigo,
        String tipoDato,
        BigDecimal valorMin,
        BigDecimal valorMax,
        List<Long> valores
) {}
