package org.mgroko.backend.contratos.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar un contrato en cualquier estado editable.
 *
 * <p>El borrador admite datos incompletos: los destinatarios pueden no estar
 * seleccionados y el texto puede generarse o quedar vacio. La emision es la
 * operacion que exige completitud.</p>
 */
public record GuardarContratoRequest(

        @NotNull(message = "El tipo de acuerdo es obligatorio.")
        Long idTipoAcuerdo,

        List<@NotNull(message = "El id de miembro no puede ser nulo.") Long> idsMiembro,

        List<
                @NotBlank(message = "La clausula adicional no puede estar vacia.")
                @Size(max = 1000, message = "Cada clausula adicional puede tener hasta 1000 caracteres.")
                String> clausulasAdicionales,

        @Size(max = 100_000, message = "El texto del contrato supera el máximo permitido.")
        String texto,

        @Size(max = 50, message = "El nombre del contrato no puede superar los 50 caracteres.")
        String nombre,

        @Size(max = 200, message = "La descripcion no puede superar los 200 caracteres.")
        String descripcion,

        ModoTextoContrato modoTexto
) {
}
