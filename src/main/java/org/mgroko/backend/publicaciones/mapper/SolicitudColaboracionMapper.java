package org.mgroko.backend.publicaciones.mapper;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
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

        return new SolicitudColaboracionBusquedaResponse(
                solicitud.getIdSolicitudColaboracion(),
                solicitud.getEstado(),
                solicitud.getFechaEnvio(),
                solicitud.getFechaRespuesta(),
                enviada
                        ? DireccionSolicitudColaboracion.ENVIADA
                        : DireccionSolicitudColaboracion.RECIBIDA,
                solicitud.getPerfilRemitente().getIdPerfil(),
                solicitud.getPerfilDestinatario().getIdPerfil(),
                toAutor(solicitud.getPerfilRemitente()),
                toAutor(solicitud.getPerfilDestinatario()),
                toPublicacionResumen(publicacion),
                pendiente && !enviada,
                pendiente && !enviada,
                pendiente && enviada);
    }

    public static SolicitudColaboracionGestionadaResponse toGestionadaResponse(
            SolicitudColaboracion solicitud,
            boolean colaboradorAgregado) {

        return new SolicitudColaboracionGestionadaResponse(
                solicitud.getIdSolicitudColaboracion(),
                solicitud.getEstado(),
                solicitud.getFechaRespuesta(),
                solicitud.getPublicacion().getIdPublicacion(),
                solicitud.getPublicacion().getTitulo(),
                colaboradorAgregado);
    }

    /**
     * Resumen de publicación reutilizado por UC-65 y por el selector de
     * publicaciones del filtro.
     */
    public static PublicacionSolicitudColaboracionResponse toPublicacionResumen(
            Publicacion publicacion) {

        String portadaUrl = publicacion.getImagenes() != null
                && !publicacion.getImagenes().isEmpty()
                        ? publicacion.getImagenes().get(0).getUrl()
                        : null;

        return new PublicacionSolicitudColaboracionResponse(
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                portadaUrl,
                publicacion.getEstado().name());
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
