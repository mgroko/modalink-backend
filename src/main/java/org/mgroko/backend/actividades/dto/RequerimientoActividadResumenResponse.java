package org.mgroko.backend.actividades.dto;

public record RequerimientoActividadResumenResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        Long cubiertos,
        Integer faltantes,
        Boolean satisfecho,
        Boolean puedePostularse
) {}
