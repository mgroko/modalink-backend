package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearObjetivoRequest(
        @NotBlank(message = "El nombre del objetivo es obligatorio.")
        @Size(max = 100, message = "El nombre del objetivo no puede superar los 100 caracteres.")
        String nombre,

        @Size(max = 300, message = "La descripción del objetivo no puede superar los 300 caracteres.")
        String descripcion
) {}
