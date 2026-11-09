package org.mgroko.backend.conexiones.mapper;

import org.mgroko.backend.conexiones.dto.DireccionSolicitudConexion;
import org.mgroko.backend.conexiones.dto.PerfilConexionResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionBusquedaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class SolicitudConexionMapper {

    private SolicitudConexionMapper() {
    }

    public static SolicitudConexionResponse toResponse(SolicitudConexion solicitud) {
        return new SolicitudConexionResponse(
                solicitud.getIdSolicitudConexion(),
                solicitud.getEstado(),
                solicitud.getFechaEnvio(),
                solicitud.getFechaRespuesta(),
                toPerfilResponse(solicitud.getPerfilRemitente()),
                toPerfilResponse(solicitud.getPerfilDestinatario()));
    }

    public static PerfilConexionResponse toPerfilResponse(Perfil perfil) {
        if (perfil == null) {
            return null;
        }

        var usuario = perfil.getUsuario();
        var profesion = perfil.getProfesion();
        var imagen = perfil.getImagen();
        var ubicacion = usuario != null ? usuario.getUbicacion() : null;

        return new PerfilConexionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getApellido() : null,
                profesion != null ? profesion.getIdProfesion() : null,
                profesion != null ? profesion.getNombre() : null,
                imagen != null ? imagen.getUrl() : null,
                UbicacionMapper.toCiudadResponse(
                        ubicacion != null ? ubicacion.getCiudad() : null));
    }

    public static SolicitudConexionBusquedaResponse toBusquedaResponse(
            SolicitudConexion solicitud,
            Long idPerfilActivo) {

        boolean enviada = solicitud.getPerfilRemitente().getIdPerfil()
                .equals(idPerfilActivo);
        DireccionSolicitudConexion direccion = enviada
                ? DireccionSolicitudConexion.ENVIADA
                : DireccionSolicitudConexion.RECIBIDA;
        Perfil contraparte = enviada
                ? solicitud.getPerfilDestinatario()
                : solicitud.getPerfilRemitente();
        boolean pendiente = solicitud.getEstado() == EstadoSolicitud.PENDIENTE;

        return new SolicitudConexionBusquedaResponse(
                solicitud.getIdSolicitudConexion(),
                solicitud.getEstado(),
                solicitud.getFechaEnvio(),
                solicitud.getFechaRespuesta(),
                direccion,
                solicitud.getPerfilRemitente().getIdPerfil(),
                solicitud.getPerfilDestinatario().getIdPerfil(),
                toPerfilResponse(contraparte),
                pendiente && !enviada,
                pendiente && !enviada,
                pendiente && enviada);
    }
}
