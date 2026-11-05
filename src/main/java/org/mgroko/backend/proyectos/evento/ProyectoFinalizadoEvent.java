package org.mgroko.backend.proyectos.evento;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando un proyecto pasa a {@code FINALIZADO}. El listener
 * de notificaciones lo consume después del commit.
 *
 * @param idProyecto            proyecto finalizado
 * @param nombreProyecto        nombre visible del proyecto
 * @param fechaFinalizacion     fecha y hora de la transición
 * @param idPerfilActor         perfil del director que finalizó el proyecto
 * @param finalizacionPrematura true si se finalizó antes de la fecha de entrega
 */
public record ProyectoFinalizadoEvent(
        Long idProyecto,
        String nombreProyecto,
        LocalDateTime fechaFinalizacion,
        Long idPerfilActor,
        boolean finalizacionPrematura) {
}
