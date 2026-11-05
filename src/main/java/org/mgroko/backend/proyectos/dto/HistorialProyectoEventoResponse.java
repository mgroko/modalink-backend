package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Evento del historial de membresías de un proyecto.
 *
 * <p>El contrato es plano: cada fila representa un movimiento de una
 * membresía. La actividad se informa de forma singular según la decisión de
 * producto de mostrar una sola actividad por evento.</p>
 */
public record HistorialProyectoEventoResponse(
        Long idEvento,
        String tipo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fecha,
        Long idPerfilAfectado,
        String nombreArtisticoAfectado,
        String nombreUsuarioAfectado,
        String apellidoUsuarioAfectado,
        String nombreProfesionAfectado,
        String rolAfectado,
        Long idUsuarioAfectado,
        Long idPerfilActor,
        String nombreArtisticoActor,
        String nombreUsuarioActor,
        String apellidoUsuarioActor,
        Long idUsuarioActor,
        String motivo,
        String estadoParticipacion,
        Long idActividad,
        String nombreActividad
) {
}
