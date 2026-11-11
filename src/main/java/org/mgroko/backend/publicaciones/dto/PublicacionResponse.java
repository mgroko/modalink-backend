package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

public record PublicacionResponse(
        Long idPublicacion,
        String titulo,
        String descripcion,
        LocalDateTime fechaPublicacion,
        String estado,
        AutorPublicacionResponse autor,
        ProyectoPublicacionResponse proyecto,
        CiudadResponse ubicacion,
        List<ImagenPublicacionResponse> imagenes,
        List<AutorPublicacionResponse> colaboradores,
        int cantidadMeGusta,
        int cantidadComentarios,
        boolean meGusta
) {
}
