package org.mgroko.backend.admin.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.dto.AdminPublicacionResponse;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

/**
 * Arma la vista administrativa de una publicación.
 */
public final class AdminPublicacionMapper {

    private AdminPublicacionMapper() {
    }

    public static AdminPublicacionResponse toResponse(
            Publicacion publicacion,
            long cantidadMeGusta,
            long cantidadComentarios,
            long cantidadReportes,
            long cantidadReportesPendientes) {

        Perfil perfil = publicacion.getPerfil();
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;

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
                colaboradores.add(toAutor(colaborador));
            }
        }

        Proyecto proyecto = publicacion.getProyecto();

        return new AdminPublicacionResponse(
                publicacion.getIdPublicacion(),
                publicacion.getTitulo(),
                publicacion.getDescripcion(),
                publicacion.getFechaPublicacion(),
                publicacion.getEstado(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                usuario != null ? usuario.getIdUsuario() : null,
                usuario != null ? nombreCompleto(usuario) : null,
                imagenes,
                proyecto != null
                        ? new ProyectoPublicacionResponse(
                                proyecto.getIdProyecto(), proyecto.getNombre(), true)
                        : null,
                publicacion.getUbicacion() != null
                        && publicacion.getUbicacion().getCiudad() != null
                                ? UbicacionMapper.toCiudadResponse(
                                        publicacion.getUbicacion().getCiudad())
                                : null,
                colaboradores,
                (int) cantidadMeGusta,
                (int) cantidadComentarios,
                cantidadReportes,
                cantidadReportesPendientes,
                publicacion.getEstado() == EstadoPublicacion.ACTIVA,
                publicacion.getEstado() == EstadoPublicacion.OCULTA);
    }

    private static AutorPublicacionResponse toAutor(Perfil perfil) {
        return new AutorPublicacionResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null,
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null);
    }

    private static String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
