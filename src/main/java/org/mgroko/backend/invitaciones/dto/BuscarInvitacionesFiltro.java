package org.mgroko.backend.invitaciones.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de UC-30. Todos son opcionales; la dirección omitida incluye
 * invitaciones recibidas y enviadas.
 */
public record BuscarInvitacionesFiltro(
        Long idProyecto,
        String nombreProyecto,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        String nombrePerfil,
        DireccionInvitacion direccion,
        EstadoSolicitud estado,
        TipoInvitacion tipo
) {
}
