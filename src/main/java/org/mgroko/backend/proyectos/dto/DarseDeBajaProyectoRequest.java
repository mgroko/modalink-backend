package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DarseDeBajaProyectoRequest(
        @NotBlank(message = "El motivo de la baja es obligatorio.")
        @Size(max = 100, message = "El motivo de la baja no puede superar los 100 caracteres.")
        String motivo
) {
}
