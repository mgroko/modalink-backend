package org.mgroko.backend.proyectos.evento;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando un proyecto pasa a {@code CONFIRMADO}. El listener
 * de notificaciones lo consume después del commit.
 */
public record ProyectoConfirmadoEvent(
        Long idProyecto,
        String nombreProyecto,
        LocalDateTime fechaConfirmacion) {
}
