package org.mgroko.backend.publicaciones.mapper;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;

public final class SolicitudColaboracionMapper {

    private SolicitudColaboracionMapper() {
    }

    public static SolicitudColaboracionResponse toResponse(SolicitudColaboracion solicitud) {
        return new SolicitudColaboracionResponse(
                solicitud.getIdSolicitudColaboracion(),
                solicitud.getEstado(),
                solicitud.getFechaEnvio(),
                solicitud.getPublicacion().getIdPublicacion(),
                solicitud.getPublicacion().getTitulo(),
                toAutor(solicitud.getPerfilRemitente()),
                toAutor(solicitud.getPerfilDestinatario()));
    }

    private static AutorPublicacionResponse toAutor(Perfil perfil) {
        if (perfil == null) {
            return null;
        }
        return new AutorPublicacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion().getNombre(),
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null);
    }
}
