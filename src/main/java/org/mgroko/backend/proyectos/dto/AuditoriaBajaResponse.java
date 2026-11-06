package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AuditoriaBajaResponse(
        Long idMiembro,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        String nombreUsuario,
        String apellidoUsuario,
        String nombreProfesion,
        String rol,
        String estadoParticipacion,
        String motivoBaja,
        LocalDateTime fechaAlta,
        LocalDateTime fechaBaja,
        AutorBajaResponse autorBaja,
        List<ActividadHistorialResponse> actividades
) {
}
