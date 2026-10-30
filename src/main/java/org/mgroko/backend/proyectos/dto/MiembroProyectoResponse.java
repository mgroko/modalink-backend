package org.mgroko.backend.proyectos.dto;

/**
 * Miembro activo de un proyecto, para la selección de destinatario de UC-70.
 */
public record MiembroProyectoResponse(
        Long idMiembro,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        String nombreUsuario,
        String apellidoUsuario,
        Long idProfesion,
        String nombreProfesion,
        String rol,
        String estadoParticipacion
) {
}
