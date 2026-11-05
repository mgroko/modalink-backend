package org.mgroko.backend.proyectos.dto;

public record ActividadHistorialResponse(
        Long idActividad,
        String nombreActividad,
        String estadoActividad,
        Long idRequerimiento,
        String nombreProfesion
) {
}
