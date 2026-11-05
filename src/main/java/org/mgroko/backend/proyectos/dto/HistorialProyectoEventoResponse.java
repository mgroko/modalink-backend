package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Evento del historial de membresías de un proyecto.
 *
 * <p>El contrato es plano: cada fila representa un movimiento de una
 * membresía. Los campos singulares {@code idActividad}/{@code nombreActividad}
 * se mantienen por compatibilidad y reflejan la primera actividad asignada;
 * {@code actividades} expone la lista completa.</p>
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
        String nombreActividad,
        List<ActividadHistorialResponse> actividades
) {
}
