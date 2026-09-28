package org.mgroko.backend.ubicacion.dto;

/**
 * Provincia de la ubicacion, reflejo de la tabla {@code provincia}.
 *
 * La clave natural es {@code (id_externo, fuente_api)}: {@code id_externo} es el
 * identificador que devuelve el catalogo externo y {@code fuente_api} identifica
 * que catalogo lo produjo. Se exponen ambos para que el cliente pueda recargar
 * la ubicacion sin depender del id interno.
 *
 * @param idProvincia id de la fila
 * @param idExterno   identificador en el catalogo de origen
 * @param fuenteApi   catalogo de origen (por ejemplo {@code GEOREF})
 * @param nombre      nombre de la provincia
 * @param pais        pais al que pertenece
 */
public record ProvinciaResponse(
        Long idProvincia,
        String idExterno,
        String fuenteApi,
        String nombre,
        PaisResponse pais
) {
}
