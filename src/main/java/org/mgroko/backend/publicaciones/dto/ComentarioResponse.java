package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

/**
 * UC-66: comentario registrado en una publicación.
 */
public record ComentarioResponse(
        Long idComentario,
        Long idPublicacion,
        AutorPublicacionResponse autor,
        String texto,
        LocalDateTime fechaComentario
) {
}
