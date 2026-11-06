package org.mgroko.backend.reemplazos.dto;

import jakarta.validation.constraints.Size;

public record CancelarReemplazoRequest(
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivo) {
}
