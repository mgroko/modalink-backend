package org.mgroko.backend.actividades.dto;

public record ProfesionalAsignadoResponse(
        Long idAsignacionActividad,
        Long idMiembroProyecto,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        Long idProfesion,
        String nombreProfesion,
        String estadoParticipacion,
        Boolean activo
) {

    public ProfesionalAsignadoResponse(
            Long idAsignacionActividad,
            Long idMiembroProyecto,
            Long idPerfil,
            Long idUsuario,
            String nombreArtistico,
            Long idProfesion,
            String nombreProfesion) {
        this(
                idAsignacionActividad,
                idMiembroProyecto,
                idPerfil,
                idUsuario,
                nombreArtistico,
                idProfesion,
                nombreProfesion,
                null,
                null);
    }
}
