package org.mgroko.backend.proyectos.dto;

import jakarta.validation.constraints.Size;

public record CrearMoodboardRequest(
        @Size(max = 200, message = "La descripción del moodboard no puede superar los 200 caracteres.")
        String descripcion
) {}
