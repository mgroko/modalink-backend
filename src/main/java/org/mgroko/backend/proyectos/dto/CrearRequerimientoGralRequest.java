package org.mgroko.backend.proyectos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearRequerimientoGralRequest(
        @NotNull(message = "La cantidad de profesionales es obligatoria.")
        @Positive(message = "La cantidad de profesionales debe ser mayor a cero.")
        Integer cantidad,

        @NotNull(message = "La profesión del requerimiento es obligatoria.")
        Long idProfesion,

        @Size(max = 200, message = "La descripción del requerimiento no puede superar los 200 caracteres.")
        String descripcion,

        List<@Valid CrearRequerimientoCaractRequest> caracteristicas,

        List<Long> habilidades
) {}
