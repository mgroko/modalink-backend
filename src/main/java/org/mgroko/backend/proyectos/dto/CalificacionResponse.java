package org.mgroko.backend.proyectos.dto;

import java.time.LocalDateTime;

/**
 * Calificación y reseña registrada por un participante del proyecto.
 */
public record CalificacionResponse(
        Long idCalificacion,
        Long idPerfilEmisor,
        Long idPerfilReceptor,
        Integer valoracion,
        String resenia,
        LocalDateTime fechaRegistro) {
}
