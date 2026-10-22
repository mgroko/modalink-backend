package org.mgroko.backend.actividades.dto;

public record ProfesionalAsignadoResponse(
        Long idAsignacionActividad,
        Long idMiembroProyecto,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        Long idProfesion,
        String nombreProfesion
) {
}
