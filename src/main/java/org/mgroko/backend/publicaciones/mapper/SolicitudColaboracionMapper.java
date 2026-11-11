package org.mgroko.backend.publicaciones.mapper;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
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

    public static SolicitudColaboracionBusquedaResponse toBusquedaResponse(
            SolicitudColaboracion solicitud,
            Long idPerfilActivo) {

        boolean enviada = solicitud.getPerfilRemitente().getIdPerfil()
                .equals(idPerfilActivo);
        boolean pendiente = solicitud.getEstado() == EstadoSolicitud.PENDIENTE;
        Publicacion publicacion = solicitud.getPublicacion();

        String portadaUrl = publicacion.getImagenes() != null
                && !publicacion.getImagenes().isEmpty()
                        ? publicacion.getImagenes().get(0).getUrl()
                        : null;

        return new SolicitudColaboracionBusquedaResponse(
                solicitud.getIdSolicitudColaboracion(),
                solicitud.getEstado(),
                solicitud.getFechaEnvio(),
                enviada
                        ? DireccionSolicitudColaboracion.ENVIADA
                        : DireccionSolicitudColaboracion.RECIBIDA,
                solicitud.getPerfilRemitente().getIdPerfil(),
                solicitud.getPerfilDestinatario().getIdPerfil(),
                toAutor(solicitud.getPerfilRemitente()),
                toAutor(solicitud.getPerfilDestinatario()),
                new PublicacionSolicitudColaboracionResponse(
                        publicacion.getIdPublicacion(),
                        publicacion.getTitulo(),
                        portadaUrl,
                        publicacion.getEstado().name()),
                pendiente && !enviada,
                pendiente && !enviada,
                pendiente && enviada);
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
