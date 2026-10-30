package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.calendario.dto.ResultadoPreferencia;

/**
 * Resultado de UC-70.
 *
 * <p>La asignación no persiste un horario: {@code inicioSeleccionado} y
 * {@code finSeleccionado} son el hueco que se usó para validar disponibilidad
 * y sólo tienen valor informativo.</p>
 *
 * @param idRequerimientoActividad requerimiento cubierto; {@code null} si el
 *                                  perfil no coincide con ninguno o si el
 *                                  miembro es director
 * @param requerimientoSatisfecho  indica si, luego de la asignación, el
 *                                  requerimiento vinculado quedó cubierto
 * @param provisional              la disponibilidad se evaluó sobre un rango
 *                                  tentativo porque el proyecto no tiene fecha
 *                                  de inicio confirmada
 */
public record AsignacionActividadResponse(
        Long idAsignacionActividad,
        Long idProyecto,
        Long idActividad,
        Long idMiembro,
        Long idPerfil,
        Long idUsuario,
        Long idRequerimientoActividad,
        boolean requerimientoSatisfecho,
        boolean provisional,
        LocalDateTime inicioSeleccionado,
        LocalDateTime finSeleccionado,
        ResultadoPreferencia resultadoPreferencia,
        Long desvioMinutos
) {
}
