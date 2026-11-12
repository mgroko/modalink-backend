package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.publicaciones.dto.AutorPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ImagenPublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionResponse;
import org.mgroko.backend.ubicacion.dto.CiudadResponse;

/**
 * Vista administrativa completa de una publicación reportada.
 *
 * <p>Incluye las imágenes y los datos que el administrador necesita para
 * moderar. No se reutiliza {@code PublicacionResponse} porque ese DTO exige rol
 * de usuario, resuelve visibilidad de proyecto y expone datos de interacción
 * del perfil consultante.</p>
 */
public record AdminPublicacionResponse(
        Long idPublicacion,
        String titulo,
        String descripcion,
        LocalDateTime fechaPublicacion,
        EstadoPublicacion estado,
        Long idPerfil,
        String nombreArtistico,
        Long idUsuario,
        String nombreUsuario,
        List<ImagenPublicacionResponse> imagenes,
        ProyectoPublicacionResponse proyecto,
        CiudadResponse ubicacion,
        List<AutorPublicacionResponse> colaboradores,
        int cantidadMeGusta,
        int cantidadComentarios,
        long cantidadReportes,
        long cantidadReportesPendientes,
        boolean puedeOcultar,
        boolean puedeReactivar
) {
}
