package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Modificación de un tipo de contrato. El código y el estado se gestionan
 * por endpoints separados para evitar cambios accidentales.
 */
public record AdminTipoContratoUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres.")
        String descripcion,

        @NotBlank(message = "La plantilla es obligatoria.")
        @Size(max = 100_000, message = "La plantilla no puede superar los 100.000 caracteres.")
        String plantilla
) {
}
