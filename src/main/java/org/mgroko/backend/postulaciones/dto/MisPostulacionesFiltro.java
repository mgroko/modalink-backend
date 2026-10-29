package org.mgroko.backend.postulaciones.dto;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Filtros opcionales del listado "mis postulaciones" (UC-27).
 */
public record MisPostulacionesFiltro(
        Long idProyecto,
        EstadoSolicitud estado,
        TipoPostulacion tipo
) {}
