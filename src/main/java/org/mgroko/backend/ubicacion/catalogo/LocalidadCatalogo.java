package org.mgroko.backend.ubicacion.catalogo;

import java.math.BigDecimal;

/**
 * Localidad en formato de dominio, independiente de la fuente de la que provenga.
 * Es el contrato que consumen los servicios: los records específicos de cada
 * API (GEOREF, GEONAMES, etc) quedan como detalle de implementación del adaptador.
 *
 * <p>El país no forma parte de este record. Reproduce lo que la fuente entrega
 * y nada más: en la base el país cuelga de la provincia
 * ({@code provincia.id_pais}), y las fuentes lo informan de formas distintas
 * (GEONAMES con {@code countryCode}, Google Maps con {@code country.short_name},
 * Georef no lo entrega). Resolverlo es responsabilidad del servicio, que usa el
 * valor de la fuente cuando viene y recurre al país por defecto cuando no.</p>
 *
 * @param idExterno       id de la localidad en la fuente de origen
 * @param nombre          nombre de la localidad
 * @param idProvincia     id de la provincia en la fuente de origen (puede ser null)
 * @param nombreProvincia nombre de la provincia (puede ser null)
 * @param fuente          catálogo de origen
 * @param latitud         latitud de referencia
 * @param longitud        longitud de referencia
 */
public record LocalidadCatalogo(
        String idExterno,
        String nombre,
        String idProvincia,
        String nombreProvincia,
        FuenteCatalogo fuente,
        BigDecimal latitud,
        BigDecimal longitud) {

    public LocalidadCatalogo {
        if (idExterno == null || idExterno.isBlank()) {
            throw new IllegalArgumentException("La localidad del catálogo debe tener id externo.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La localidad del catálogo debe tener nombre.");
        }
        if (fuente == null) {
            throw new IllegalArgumentException("La localidad del catálogo debe indicar su fuente.");
        }
    }
}
