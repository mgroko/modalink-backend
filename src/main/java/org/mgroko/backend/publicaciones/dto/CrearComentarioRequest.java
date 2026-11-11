package org.mgroko.backend.publicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC-66: texto del comentario a registrar.
 */
public record CrearComentarioRequest(
        @NotBlank(message = "El comentario no puede estar vacío.")
        @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres.")
        String texto
) {
}
