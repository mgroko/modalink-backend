package org.mgroko.backend.publicaciones.mapper;

import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;

public final class ComentarioMapper {

    private ComentarioMapper() {
    }

    public static ComentarioResponse toResponse(Comentario comentario) {
        AutorPublicacionResponse autor = PublicacionMapper.toAutor(comentario.getPerfil());

        if (comentario.getNombreArtisticoAutor() != null) {
            autor = new AutorPublicacionResponse(
                    autor.idPerfil(),
                    comentario.getNombreArtisticoAutor(),
                    autor.profesion(),
                    autor.fotoUrl(),
                    autor.origen());
        }

        return new ComentarioResponse(
                comentario.getIdComentario(),
                comentario.getPublicacion().getIdPublicacion(),
                autor,
                comentario.getTexto(),
                comentario.getFechaComentario());
    }
}
