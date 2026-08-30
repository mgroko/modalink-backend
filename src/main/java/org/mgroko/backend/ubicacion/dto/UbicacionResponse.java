package org.mgroko.backend.ubicacion.dto;

import java.math.BigDecimal;

/**
 * Ubicación del usuario expuesta a la API.
 *
 * @param idUbicacion  id de la fila en la tabla {@code ubicacion}
 * @param localidadId  id de la localidad en el catálogo de Georef (para prellenar el selector)
 * @param localidad    nombre de la localidad
 * @param provincia    nombre de la provincia
 * @param pais         país de la ubicación
 * @param codigoPostal código postal (opcional)
 * @param latitud      latitud del centroide
 * @param longitud     longitud del centroide
 */
public record UbicacionResponse(
        Long idUbicacion,
        String localidadId,
        String localidad,
        String provincia,
        String pais,
        String codigoPostal,
        BigDecimal latitud,
        BigDecimal longitud
) {
}