package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoPublicacion;

/**
 * Vista reducida de una publicación oculta para su dueño.
 *
 * <p>El dueño solo puede ver el título y la descripción desde la búsqueda
 * específica de publicaciones ocultas. No se exponen imágenes, proyecto,
 * ubicación, interacciones ni colaboradores.</p>
 */
public record PublicacionOcultadaResponse(
        Long idPublicacion,
        String titulo,
        String descripcion,
        LocalDateTime fechaPublicacion,
        EstadoPublicacion estado
) {
}
