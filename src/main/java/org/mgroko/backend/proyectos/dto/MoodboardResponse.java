package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MoodboardResponse(
        Long idMoodboard,
        String descripcion,
        LocalDateTime fechaCreacion,
        List<ImagenMoodboardResponse> imagenes
) {}
