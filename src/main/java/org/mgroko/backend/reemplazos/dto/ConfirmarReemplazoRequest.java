package org.mgroko.backend.reemplazos.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmarReemplazoRequest(
        @NotEmpty(message = "Debés seleccionar al menos un candidato.")
        List<@NotNull(message = "El candidato no puede ser nulo.") Long> idsCandidatos,

        @Size(max = 200, message = "El mensaje no puede superar los 200 caracteres.")
        String mensaje) {
}
