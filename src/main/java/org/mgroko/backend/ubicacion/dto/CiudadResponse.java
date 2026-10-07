package org.mgroko.backend.ubicacion.dto;

/**
 * Ciudad de la ubicacion, reflejo de la tabla {@code ciudad}.
 *
 * La clave natural es {@code (id_externo, fuente_api)}, igual que en provincia.
 * No se exponen {@code codigo_postal}, {@code latitud_defecto} ni
 * {@code longitud_defecto}: son datos de mantenimiento del catalogo, y la
 * postal y las coordenadas propias viajan en {@link UbicacionResponse}.
 *
 * @param idCiudad   id de la fila
 * @param idExterno  identificador en el catalogo de origen
 * @param fuenteApi  catalogo de origen (por ejemplo {@code GEOREF})
 * @param nombre     nombre de la ciudad
 * @param provincia  provincia a la que pertenece
 */
public record CiudadResponse(
        Long idCiudad,
        String idExterno,
        String fuenteApi,
        String nombre,
        ProvinciaResponse provincia
) {
}
