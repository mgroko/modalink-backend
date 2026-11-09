package org.mgroko.backend.requerimientos.dto;

import java.util.List;

/**
 * Filtros del buscador global de requerimientos abiertos (UC-72/UC-73).
 *
 * <p>Todos los criterios son opcionales: sin filtros se listan todas las
 * oportunidades visibles, priorizando las de la profesión del perfil activo.</p>
 *
 * <p>La ubicación se expresa con los identificadores externos del catálogo
 * Georef ({@code provinciaId}, {@code localidadId}), que son los que expone
 * la API pública de ubicaciones y los que la base persiste en
 * {@code ciudad.id_externo}/{@code provincia.id_externo}.</p>
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
        Boolean excluirYaPostulados,
        String provinciaId,
        String localidadId
) {

    /**
     * Constructor de compatibilidad con la firma anterior (sin ubicación).
     */
    public BuscarRequerimientosGlobalFiltro(
            Long idProfesion,
            Long idProyecto,
            Long idActividad,
            String texto,
            Long idCaracteristica,
            Long idValorCaracteristica,
            List<Long> idsHabilidades,
            Boolean soloMiProfesion,
            Boolean soloPostulables,
            Boolean excluirYaPostulados) {
        this(idProfesion, idProyecto, idActividad, texto, idCaracteristica,
                idValorCaracteristica, idsHabilidades, soloMiProfesion,
                soloPostulables, excluirYaPostulados, null, null);
    }
}
