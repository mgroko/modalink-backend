package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de la pestaña de colaboraciones automáticas.
 */
public record BuscarColaboracionesAutomaticasFiltro(
        EstadoColaboracionAutomatica estado,
        Boolean todosLosEstados,
        Long idPublicacion,
        Long idPerfilContraparte,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
) {
}
