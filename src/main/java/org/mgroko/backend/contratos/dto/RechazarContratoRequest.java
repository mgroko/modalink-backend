package org.mgroko.backend.contratos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarContratoRequest(

        @NotBlank(message = "El motivo de rechazo es obligatorio.")
        @Size(max = 200, message = "El motivo de rechazo no puede superar los 200 caracteres.")
        String motivo
) {
}
