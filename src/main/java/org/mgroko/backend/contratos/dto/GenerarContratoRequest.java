package org.mgroko.backend.contratos.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que envia el director para previsualizar o generar un contrato.
 * Se usa en ambos endpoints porque la previsualizacion debe ser identica a la
 * generacion final.
 */
public record GenerarContratoRequest(

        @NotNull(message = "El tipo de acuerdo es obligatorio.")
        Long idTipoAcuerdo,

        @NotEmpty(message = "Debe seleccionar al menos un miembro destinatario.")
        List<@NotNull(message = "El id de miembro no puede ser nulo.") Long> idsMiembro,

        List<
                @NotBlank(message = "La clausula adicional no puede estar vacia.")
                @Size(max = 1000, message = "Cada clausula adicional puede tener hasta 1000 caracteres.")
                String> clausulasAdicionales
) {
}
