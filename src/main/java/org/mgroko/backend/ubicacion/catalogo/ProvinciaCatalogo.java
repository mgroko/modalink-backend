package org.mgroko.backend.ubicacion.catalogo;

import java.math.BigDecimal;

/**
 * Provincia en formato de dominio, independiente de la fuente de la que provenga.
 * Contrapartida de {@link LocalidadCatalogo} para la jerarquía
 * pais -> provincia -> ciudad -> ubicacion.
 *
 * <p>No incluye el país por el mismo motivo que {@link LocalidadCatalogo}: la
 * fuente puede no entregarlo, y en la base el país es la FK
 * {@code provincia.id_pais}, no un atributo de la provincia.</p>
 *
 * @param idExterno id de la provincia en la fuente de origen
 * @param nombre    nombre de la provincia
 * @param fuente    catálogo de origen
 * @param latitud   latitud del centroide
 * @param longitud  longitud del centroide
 */
public record ProvinciaCatalogo(
        String idExterno,
        String nombre,
        FuenteCatalogo fuente,
        BigDecimal latitud,
        BigDecimal longitud) {

    public ProvinciaCatalogo {
        if (idExterno == null || idExterno.isBlank()) {
            throw new IllegalArgumentException("La provincia del catálogo debe tener id externo.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La provincia del catálogo debe tener nombre.");
        }
        if (fuente == null) {
            throw new IllegalArgumentException("La provincia del catálogo debe indicar su fuente.");
        }
    }
}
