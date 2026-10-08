package org.mgroko.backend.ubicacion.dto;

import java.math.BigDecimal;

/**
 * Ubicacion del usuario, reflejo de la tabla {@code ubicacion}.
 *
 * La tabla solo guarda la direccion y las coordenadas: la localidad, la
 * provincia y el pais se alcanzan por la cadena de claves foraneas
 * {@code ubicacion.id_ciudad -> ciudad.id_provincia -> provincia.id_pais}.
 * Por eso la respuesta anida la ciudad en vez de exponer locality/provincia/pais
 * como cadenas planas, que era lo que hacia la entidad {@code Ubicacion} con sus
 * getters helper.
 *
 * @param idUbicacion  id de la fila
 * @param direccion    direccion en texto libre
 * @param codigoPostal codigo postal
 * @param latitud      latitud
 * @param longitud     longitud
 * @param ciudad       ciudad de la ubicacion
 */
public record UbicacionResponse(
        Long idUbicacion,
        String direccion,
        String codigoPostal,
        BigDecimal latitud,
        BigDecimal longitud,
        CiudadResponse ciudad
) {
}
