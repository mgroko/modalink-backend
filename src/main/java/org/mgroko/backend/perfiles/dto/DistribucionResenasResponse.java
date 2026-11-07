package org.mgroko.backend.perfiles.dto;

/**
 * Cantidad de reseñas para una valoración puntual (1 a 5).
 */
public record DistribucionResenasResponse(
        Integer valoracion,
        long cantidad) {
}
