package org.mgroko.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de un tipo de contrato. El código se genera automáticamente a partir
 * del nombre y no se recibe desde el cliente.
 */
public record AdminTipoContratoCreateRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres.")
        String descripcion,

        @NotBlank(message = "La plantilla es obligatoria.")
        @Size(max = 100_000, message = "La plantilla no puede superar los 100.000 caracteres.")
        String plantilla,

        Boolean activo
) {
}
