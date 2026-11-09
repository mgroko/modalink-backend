package org.mgroko.backend.contratos.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de UC-53. Si {@code texto} viene informado se usa como texto final;
 * si no, el backend regenera desde la plantilla y las clausulas adicionales.
 */
public record ModificarContratoRequest(

        @NotNull(message = "El tipo de acuerdo es obligatorio.")
        Long idTipoAcuerdo,

        @NotEmpty(message = "Debe seleccionar al menos un miembro destinatario.")
        List<@NotNull(message = "El id de miembro no puede ser nulo.") Long> idsMiembro,

        @Size(max = 100_000, message = "El texto del contrato supera el máximo permitido.")
        String texto,

        List<
                @NotBlank(message = "La clausula adicional no puede estar vacia.")
                @Size(max = 1000, message = "Cada clausula adicional puede tener hasta 1000 caracteres.")
                String> clausulasAdicionales,

        @Size(max = 50, message = "El nombre del contrato no puede superar los 50 caracteres.")
        String nombre,

        @Size(max = 200, message = "La descripcion no puede superar los 200 caracteres.")
        String descripcion
) {
}
