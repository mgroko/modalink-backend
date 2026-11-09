package org.mgroko.backend.mensajeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(
        @NotBlank(message = "El mensaje no puede estar vacío.")
        @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres.")
        String contenido
) {
}
