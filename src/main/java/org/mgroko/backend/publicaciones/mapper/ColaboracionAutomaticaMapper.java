package org.mgroko.backend.publicaciones.mapper;

import java.util.List;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;
import org.mgroko.backend.publicaciones.dto.ColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.dto.ImagenColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionColaboracionAutomaticaResponse;

public final class ColaboracionAutomaticaMapper {

    private ColaboracionAutomaticaMapper() {
    }

    public static ColaboracionAutomaticaResponse toResponse(
            ColaboracionAutomatica colaboracion,
            Long idPerfilActivo) {

        boolean esPerfilA = colaboracion.getPerfilA().getIdPerfil()
                .equals(idPerfilActivo);

        Publicacion publicacionPropia = esPerfilA
                ? colaboracion.getPublicacionA()
                : colaboracion.getPublicacionB();
        Publicacion publicacionContraparte = esPerfilA
                ? colaboracion.getPublicacionB()
                : colaboracion.getPublicacionA();
        Perfil perfilContraparte = esPerfilA
                ? colaboracion.getPerfilB()
                : colaboracion.getPerfilA();

        EstadoConfirmacionColaboracion miEstado = esPerfilA
                ? colaboracion.getEstadoPerfilA()
                : colaboracion.getEstadoPerfilB();
        EstadoConfirmacionColaboracion estadoContraparte = esPerfilA
                ? colaboracion.getEstadoPerfilB()
                : colaboracion.getEstadoPerfilA();

        boolean pendiente = colaboracion.getEstado()
                == EstadoColaboracionAutomatica.PENDIENTE;

        return new ColaboracionAutomaticaResponse(
                colaboracion.getIdColaboracionAutomatica(),
                colaboracion.getEstado(),
                miEstado,
                estadoContraparte,
                colaboracion.getFechaDeteccion(),
                colaboracion.getFechaConfirmacion(),
                colaboracion.getFechaDisolucion(),
                toPublicacion(publicacionPropia),
                toPublicacion(publicacionContraparte),
                PublicacionMapper.toAutor(perfilContraparte),
                toImagen(colaboracion.getImagen()),
                pendiente && miEstado == EstadoConfirmacionColaboracion.PENDIENTE,
                pendiente && miEstado == EstadoConfirmacionColaboracion.PENDIENTE);
    }

    private static PublicacionColaboracionAutomaticaResponse toPublicacion(
            Publicacion publicacion) {

        if (publicacion == null) {
            return new PublicacionColaboracionAutomaticaResponse(
                    null, null, null, "ELIMINADA", true);
        }

        List<Imagen> imagenes = publicacion.getImagenes();
        String portadaUrl = imagenes != null && !imagenes.isEmpty()
                ? imagenes.get(0).getUrl()
                : null;

        return new PublicacionColaboracionAutomaticaResponse(
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                portadaUrl,
                publicacion.getEstado().name(),
                false);
    }

    private static ImagenColaboracionAutomaticaResponse toImagen(Imagen imagen) {
        if (imagen == null) {
            return null;
        }
        return new ImagenColaboracionAutomaticaResponse(
                imagen.getIdImagen(),
                imagen.getUrl());
    }
}
