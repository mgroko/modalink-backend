package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarProyectoRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio.")
        @Size(max = 200, message = "El motivo de cancelación no puede superar los 200 caracteres.")
        String motivoCancelacion
) {}
