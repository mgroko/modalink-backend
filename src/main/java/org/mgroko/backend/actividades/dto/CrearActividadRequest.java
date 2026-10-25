package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearActividadRequest(
        @NotBlank(message = "El nombre de la actividad es obligatorio.")
        @Size(max = 20, message = "El nombre no puede superar los 20 caracteres.")
        String nombre,

        @Size(max = 200, message = "La descripcion no puede superar los 200 caracteres.")
        String descripcion,

        @NotNull(message = "La duracion es obligatoria.")
        @Positive(message = "La duracion debe ser mayor que cero.")
        Integer duracion,

        @NotNull(message = "La unidad de duracion es obligatoria.")
        UnidadDuracion unidadDuracion,

        @Valid
        UbicacionRequest ubicacion,

        List<Long> predecesoras,

        List<@Valid CrearRequerimientoActividadRequest> requerimientos
) {
}
