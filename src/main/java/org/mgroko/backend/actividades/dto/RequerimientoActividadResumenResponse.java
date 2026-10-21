package org.mgroko.backend.actividades.dto;

public record RequerimientoActividadResumenResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion
) {}
