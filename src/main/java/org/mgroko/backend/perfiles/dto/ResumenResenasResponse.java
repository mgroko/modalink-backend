package org.mgroko.backend.perfiles.dto;

import java.util.List;

/**
 * Resumen agregado de las reseñas recibidas por un perfil.
 */
public record ResumenResenasResponse(
        double promedio,
        long total,
        List<DistribucionResenasResponse> distribucion) {
}
