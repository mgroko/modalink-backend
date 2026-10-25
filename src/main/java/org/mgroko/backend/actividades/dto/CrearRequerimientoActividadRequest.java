package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearRequerimientoActividadRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        Integer cantidad,

        @NotNull(message = "La profesion es obligatoria.")
        Long idProfesion,

        @Size(max = 200, message = "La descripcion no puede superar los 200 caracteres.")
        String descripcion,

        List<@Valid CrearRequerimientoCaractRequest> caracteristicas,

        List<Long> habilidades
) {
}
