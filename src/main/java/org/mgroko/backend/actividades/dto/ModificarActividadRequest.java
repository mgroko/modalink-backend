package org.mgroko.backend.actividades.dto;

import java.util.List;
import java.time.LocalTime;

import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ModificarActividadRequest(
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

        @JsonFormat(pattern = "HH:mm")
        LocalTime horarioPreferenciaInicio,

        @Valid
        UbicacionRequest ubicacion,

        @NotNull(message = "La lista de predecesoras es obligatoria.")
        List<@NotNull(message = "La lista de predecesoras no puede contener valores nulos.")
        Long> predecesoras,

        @NotNull(message = "La lista de requerimientos es obligatoria.")
        List<@NotNull(message = "La lista de requerimientos no puede contener valores nulos.")
        @Valid ModificarRequerimientoActividadRequest> requerimientos,

        @NotNull(message = "El estado es obligatorio.")
        EstadoActividad estado
) {

    public ModificarActividadRequest(
            String nombre,
            String descripcion,
            Integer duracion,
            UnidadDuracion unidadDuracion,
            UbicacionRequest ubicacion,
            List<Long> predecesoras,
            List<@NotNull(message = "La lista de requerimientos no puede contener valores nulos.")
                    @Valid ModificarRequerimientoActividadRequest> requerimientos,
            EstadoActividad estado) {
        this(nombre, descripcion, duracion, unidadDuracion, null,
                ubicacion, predecesoras, requerimientos, estado);
    }
}
