package org.mgroko.backend.publicaciones.mapper;

import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;

public final class ComentarioMapper {

    private ComentarioMapper() {
    }

    public static ComentarioResponse toResponse(Comentario comentario) {
        return new ComentarioResponse(
                comentario.getIdComentario(),
                comentario.getPublicacion().getIdPublicacion(),
                PublicacionMapper.toAutor(comentario.getPerfil()),
                comentario.getTexto(),
                comentario.getFechaComentario());
    }
}
