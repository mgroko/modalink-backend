package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CupoRequerimiento;

/**
 * Resultado de la desasignación de un miembro de una actividad.
 *
 * @param idRequerimientoActividad requerimiento que cubría la asignación;
 *                                  {@code null} si no estaba vinculada
 * @param requerimientoSatisfecho  estado del requerimiento después de la baja
 * @param cupo                     cupo recalculado; {@code null} si no había
 *                                  requerimiento vinculado
 * @param advertencias             avisos no bloqueantes, por ejemplo un
 *                                  requerimiento que quedó insaturado
 */
public record DesasignacionActividadResponse(
        Long idAsignacionActividad,
        Long idProyecto,
        Long idActividad,
        Long idMiembro,
        Long idPerfil,
        Long idUsuario,
        Long idRequerimientoActividad,
        boolean requerimientoSatisfecho,
        CupoRequerimiento cupo,
        List<String> advertencias
) {
    public DesasignacionActividadResponse {
        advertencias = advertencias == null ? List.of() : List.copyOf(advertencias);
    }
}
