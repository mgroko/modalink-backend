package org.mgroko.backend.actividades.dto;

import java.util.List;

/**
 * Filtros de UC-73: búsqueda de requerimientos de actividad dentro de un
 * proyecto. El proyecto es obligatorio en la ruta, por lo que siempre existe
 * al menos un criterio de búsqueda.
 */
public record BuscarRequerimientosActividadFiltro(
        Long idActividad,
        Long idProfesion,
        Long idCaracteristica,
        Long idValorCaracteristica,
        List<Long> idsHabilidades
) {
}
