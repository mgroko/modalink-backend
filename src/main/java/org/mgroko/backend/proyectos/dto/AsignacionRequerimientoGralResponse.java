package org.mgroko.backend.proyectos.dto;

/**
 * Resultado de asignar, reasignar o desasignar un requerimiento general.
 */
public record AsignacionRequerimientoGralResponse(
        Long idMiembro,
        Long idPerfil,
        Long idRequerimientoGral,
        CupoRequerimiento cupo,
        Boolean satisfecho,
        Integer faltantes
) {
}
