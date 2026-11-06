package org.mgroko.backend.requerimientos.dto;

import java.util.List;

/**
 * Filtros del buscador global de requerimientos abiertos (UC-72/UC-73).
 *
 * <p>Todos los criterios son opcionales: sin filtros se listan todas las
 * oportunidades visibles, priorizando las de la profesión del perfil activo.</p>
 */
public record BuscarRequerimientosGlobalFiltro(
        Long idProfesion,
        Long idProyecto,
        Long idActividad,
        String texto,
        Long idCaracteristica,
        Long idValorCaracteristica,
        List<Long> idsHabilidades,
        Boolean soloMiProfesion,
        Boolean soloPostulables,
        Boolean excluirYaPostulados
) {
}
