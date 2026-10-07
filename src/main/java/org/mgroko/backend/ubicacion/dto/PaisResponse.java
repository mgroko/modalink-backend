package org.mgroko.backend.ubicacion.dto;

/**
 * Pais de la ubicacion, reflejo de la tabla {@code pais}.
 *
 * No se expone {@code activo}: es una bandera de mantenimiento del catalogo y
 * no aporta informacion al consumidor de la ubicacion.
 *
 * @param idPais    id de la fila
 * @param codigoIso codigo ISO 3166-1 alfa-2 (clave natural de la tabla)
 * @param nombre    nombre del pais
 */
public record PaisResponse(
        Long idPais,
        String codigoIso,
        String nombre
) {
}
