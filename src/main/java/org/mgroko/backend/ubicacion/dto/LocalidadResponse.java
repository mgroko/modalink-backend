package org.mgroko.backend.ubicacion.dto;

import java.math.BigDecimal;

/**
 * Localidad del catálogo de Georef expuesta a la API.
 *
 * @param id             id de la localidad en el catálogo de Georef
 * @param nombre         nombre de la localidad
 * @param provinciaId    id de la provincia (Georef)
 * @param provinciaNombre nombre de la provincia
 * @param latitud        latitud del centroide
 * @param longitud       longitud del centroide
 */
public record LocalidadResponse(
        String id,
        String nombre,
        String provinciaId,
        String provinciaNombre,
        BigDecimal latitud,
        BigDecimal longitud
) {
}