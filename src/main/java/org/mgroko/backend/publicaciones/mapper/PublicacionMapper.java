package org.mgroko.backend.publicaciones.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
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
        return toResponse(
                publicacion, puedeVerProyecto, ResumenInteraccionPublicacion.vacio(), Map.of());
    }

    /**
     * UC-66: tarjeta de publicación con los contadores y el estado de Me gusta
     * del perfil que consulta.
     */
    public static PublicacionResponse toResponse(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            ResumenInteraccionPublicacion resumen) {
        return toResponse(publicacion, puedeVerProyecto, resumen, Map.of());
    }

    public static PublicacionResponse toResponse(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            ResumenInteraccionPublicacion resumen,
            Map<Long, OrigenColaboracion> origenesColaboradores) {
        return toResponse(publicacion, puedeVerProyecto, false, resumen, origenesColaboradores);
    }

    /**
     * UC-21: respuesta para el dueño de la publicación. A diferencia del
     * listado público, incluye el proyecto asociado aunque el perfil ya no
     * tenga visibilidad sobre él, para que el formulario pueda conservarlo.
     */
    public static PublicacionResponse toResponsePropia(Publicacion publicacion, boolean puedeVerProyecto) {
        return toResponsePropia(
                publicacion, puedeVerProyecto, ResumenInteraccionPublicacion.vacio(), Map.of());
    }

    public static PublicacionResponse toResponsePropia(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            ResumenInteraccionPublicacion resumen) {
        return toResponsePropia(publicacion, puedeVerProyecto, resumen, Map.of());
    }

    public static PublicacionResponse toResponsePropia(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            ResumenInteraccionPublicacion resumen,
            Map<Long, OrigenColaboracion> origenesColaboradores) {
        return toResponse(
                publicacion, puedeVerProyecto, true, resumen, origenesColaboradores);
    }

    private static PublicacionResponse toResponse(
            Publicacion publicacion,
            boolean puedeVerProyecto,
            boolean incluirProyectoNoVisible,
            ResumenInteraccionPublicacion resumen,
            Map<Long, OrigenColaboracion> origenesColaboradores) {

        ResumenInteraccionPublicacion interaccion = resumen != null
                ? resumen
                : ResumenInteraccionPublicacion.vacio();

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
                    colaboradores.add(toAutor(
                            colaborador,
                            origenesColaboradores != null
                                    ? origenesColaboradores.get(colaborador.getIdPerfil())
                                    : null));
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
                interaccion.cantidadMeGusta(),
                interaccion.cantidadComentarios(),
                interaccion.meGusta());
    }

    public static AutorPublicacionResponse toAutor(Perfil perfil) {
        return toAutor(perfil, null);
    }

    public static AutorPublicacionResponse toAutor(
            Perfil perfil,
            OrigenColaboracion origen) {
        return new AutorPublicacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion().getNombre(),
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null,
                origen != null ? origen.name() : null);
    }
}
