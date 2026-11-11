package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

public record HistorialProyectoPerfilResponse(
        Long idProyecto,
        String nombre,
        String estado,
        String rol,
        String rolCodigo,
        LocalDateTime fechaFinalizacion,
        LocalDateTime fechaCancelacion,
        boolean puedeVerProyecto
) {
}
