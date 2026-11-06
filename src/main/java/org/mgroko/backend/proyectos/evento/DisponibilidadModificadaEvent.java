package org.mgroko.backend.proyectos.evento;

import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;

/**
 * Evento publicado después de un cambio de disponibilidad de un usuario.
 *
 * <p>El listener lo consume después del commit y reprograma los proyectos
 * publicados o confirmados del usuario, excluyendo el proyecto que ya fue
 * reprogramado por el flujo que originó el cambio.</p>
 *
 * @param idUsuario          usuario cuya disponibilidad cambió
 * @param motivo             motivo de la reprogramación
 * @param idProyectoExcluido proyecto que no debe volver a reprogramarse;
 *                           {@code null} si no hay exclusión
 */
public record DisponibilidadModificadaEvent(
        Long idUsuario,
        MotivoReprogramacion motivo,
        Long idProyectoExcluido) {
}
