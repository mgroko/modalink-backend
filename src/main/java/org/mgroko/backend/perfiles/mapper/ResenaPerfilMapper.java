package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.dto.AutorResenaResponse;
import org.mgroko.backend.perfiles.dto.ProyectoResenaResponse;
import org.mgroko.backend.perfiles.dto.ResenaPerfilResponse;

/**
 * Mapper de reseñas de perfil. La entidad se llama {@link CalificacionPerfil}
 * porque nace en el flujo de finalización de proyecto (UC-37), pero de cara al
 * perfil se expone como reseña.
 */
public final class ResenaPerfilMapper {

    private ResenaPerfilMapper() {
    }

    public static ResenaPerfilResponse toResponse(CalificacionPerfil calificacion) {
        Perfil emisor = calificacion.getPerfilEmisor();

        return new ResenaPerfilResponse(
                calificacion.getIdCalificacion(),
                calificacion.getValoracion(),
                calificacion.getResenia(),
                calificacion.getFechaRegistro(),
                new AutorResenaResponse(
                        emisor != null ? emisor.getIdPerfil() : null,
                        emisor != null ? emisor.getNombreArtistico() : null,
                        emisor != null && emisor.getProfesion() != null
                                ? emisor.getProfesion().getNombre()
                                : null,
                        emisor != null && emisor.getImagen() != null
                                ? emisor.getImagen().getUrl()
                                : null),
                new ProyectoResenaResponse(
                        calificacion.getProyecto() != null
                                ? calificacion.getProyecto().getIdProyecto()
                                : null,
                        calificacion.getProyecto() != null
                                ? calificacion.getProyecto().getNombre()
                                : null));
    }
}
