package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

/**
 * Reseña recibida por un perfil. Se usa tanto en el listado como en el
 * detalle de la reseña.
 */
public record ResenaPerfilResponse(
        Long idCalificacion,
        Integer valoracion,
        String resenia,
        LocalDateTime fechaRegistro,
        AutorResenaResponse emisor,
        ProyectoResenaResponse proyecto) {
}
