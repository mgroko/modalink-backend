package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearProyectoRequest(
        @NotBlank(message = "El nombre del proyecto es obligatorio.")
        @Size(max = 50, message = "El nombre del proyecto no puede superar los 50 caracteres.")
        String nombre,

        @NotBlank(message = "La descripción del proyecto es obligatoria.")
        @Size(max = 200, message = "La descripción del proyecto no puede superar los 200 caracteres.")
        String descripcion,

        @NotNull(message = "La privacidad del proyecto es obligatoria.")
        Privacidad privacidad,

        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,

        LocalDate fechaFinEstipulada,

        Boolean aceptaPostulacionGral,

        @Valid
        UbicacionRequest ubicacion,

        List<@Valid CrearObjetivoRequest> objetivos
) {}
