package org.mgroko.backend.ubicacion.dto;

import java.math.BigDecimal;

/**
 * Localidad del catálogo geográfico expuesta a la API.
 * Desacoplada de la fuente: el mismo contrato sirve para cualquier catálogo.
 *
 * @param id             id de la localidad en el catálogo de origen
 * @param nombre         nombre de la localidad
 * @param provinciaId    id de la provincia en el catálogo de origen
 * @param provinciaNombre nombre de la provincia
 * @param latitud        latitud de referencia
 * @param longitud       longitud de referencia
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