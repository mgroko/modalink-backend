package org.mgroko.backend.publicaciones.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class PublicacionMapper {

    private PublicacionMapper() {
    }

    /**
     * Construye la tarjeta de publicación. Si el viewer no puede ver el
     * proyecto asociado, se omite su nombre para no filtrar datos de proyectos
     * privados.
     */
    public static PublicacionResponse toResponse(Publicacion publicacion, boolean puedeVerProyecto) {
        return toResponse(publicacion, puedeVerProyecto, false);
    }

    /**
     * UC-21: respuesta para el dueño de la publicación. A diferencia del
     * listado público, incluye el proyecto asociado aunque el perfil ya no
     * tenga visibilidad sobre él, para que el formulario pueda conservarlo.
     */
    public static PublicacionResponse toResponsePropia(Publicacion publicacion, boolean puedeVerProyecto) {
        return toResponse(publicacion, puedeVerProyecto, true);
    }

    private static PublicacionResponse toResponse(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            boolean incluirProyectoNoVisible) {

        Perfil perfil = publicacion.getPerfil();
        AutorPublicacionResponse autor = toAutor(perfil);

        ProyectoPublicacionResponse proyecto = null;
        Proyecto proyectoAsociado = publicacion.getProyecto();
        if (proyectoAsociado != null && (puedeVerProyecto || incluirProyectoNoVisible)) {
            proyecto = new ProyectoPublicacionResponse(
                    proyectoAsociado.getIdProyecto(),
                    proyectoAsociado.getNombre(),
                    puedeVerProyecto);
        }

        List<ImagenPublicacionResponse> imagenes = new ArrayList<>();
        if (publicacion.getImagenes() != null) {
            int orden = 0;
            for (Imagen imagen : publicacion.getImagenes()) {
                imagenes.add(new ImagenPublicacionResponse(
                        imagen.getIdImagen(),
                        imagen.getUrl(),
                        orden++));
            }
        }

        List<AutorPublicacionResponse> colaboradores = new ArrayList<>();
        if (publicacion.getColaboradores() != null) {
            for (Perfil colaborador : publicacion.getColaboradores()) {
                // Los perfiles en baja no se exponen, igual que en UC-14.
                if (colaborador.getEstado() == EstadoPerfil.Activo) {
                    colaboradores.add(toAutor(colaborador));
                }
            }
        }

        return new PublicacionResponse(
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                publicacion.getDescripcion(),
                publicacion.getFechaPublicacion(),
                publicacion.getEstado().name(),
                autor,
                proyecto,
                publicacion.getUbicacion() != null
                        ? UbicacionMapper.toCiudadResponse(publicacion.getUbicacion().getCiudad())
                        : null,
                imagenes,
                colaboradores,
                0,
                0);
    }

    private static AutorPublicacionResponse toAutor(Perfil perfil) {
        return new AutorPublicacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion().getNombre(),
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null);
    }
}
