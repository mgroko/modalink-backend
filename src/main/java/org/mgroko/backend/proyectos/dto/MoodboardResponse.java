package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

public record MoodboardResponse(
        Long idMoodboard,
        String descripcion,
        LocalDateTime fechaCreacion
) {}
