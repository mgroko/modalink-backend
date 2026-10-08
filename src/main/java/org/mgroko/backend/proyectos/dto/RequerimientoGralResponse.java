package org.mgroko.backend.proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

public record RequerimientoGralResponse(
        Long idRequerimientoGral,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades
) {}
