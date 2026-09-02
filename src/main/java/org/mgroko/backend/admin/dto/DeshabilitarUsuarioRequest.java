package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos que el administrador registra al deshabilitar un usuario (UC-04).
 * El motivo es obligatorio; la duración es opcional: si no se indica, la
 * deshabilitación es indefinida y solo se revierte con {@code /habilitar}.
 */
public record DeshabilitarUsuarioRequest(
        @NotBlank(message = "El motivo de la deshabilitación es obligatorio.")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivo,

        @Positive(message = "La duración debe ser un número positivo de días.")
        Integer duracionDias
) {
}