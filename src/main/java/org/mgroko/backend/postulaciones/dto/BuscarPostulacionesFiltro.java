package org.mgroko.backend.postulaciones.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de UC-74: búsqueda de postulaciones dentro de un proyecto.
 * Todos los campos son opcionales.
 */
public record BuscarPostulacionesFiltro(
        List<EstadoSolicitud> estado,
        TipoPostulacion tipo,
        String nombrePerfil,
        Long idProfesion,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

    /**
     * Devuelve una copia del filtro sin el estado. Se usa para calcular los
     * conteos por estado, que deben incluir los tres valores a la vez.
     *
     * @return filtro equivalente sin {@code estado}
     */
    public BuscarPostulacionesFiltro sinEstado() {
        return new BuscarPostulacionesFiltro(
                null, tipo, nombrePerfil, idProfesion, fechaDesde, fechaHasta);
    }
}
