package org.mgroko.backend.proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CrearRequerimientoCaractRequest(
        @NotNull(message = "El id de la característica es obligatorio.")
        Long idCaracteristica,

        @PositiveOrZero(message = "El valor mínimo no puede ser negativo.")
        BigDecimal valorMin,

        @PositiveOrZero(message = "El valor máximo no puede ser negativo.")
        BigDecimal valorMax,

        List<Long> valores
) {}
