package org.mgroko.backend.publicaciones.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
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
        Perfil perfil = publicacion.getPerfil();

        AutorPublicacionResponse autor = new AutorPublicacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion().getNombre(),
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null);

        ProyectoPublicacionResponse proyecto = null;
        if (publicacion.getProyecto() != null && puedeVerProyecto) {
            proyecto = new ProyectoPublicacionResponse(
                    publicacion.getProyecto().getIdProyecto(),
                    publicacion.getProyecto().getNombre(),
                    true);
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
                List.of(),
                0,
                0);
    }
}
