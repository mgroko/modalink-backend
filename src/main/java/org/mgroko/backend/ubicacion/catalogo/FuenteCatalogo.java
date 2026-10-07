package org.mgroko.backend.ubicacion.catalogo;

/**
 * Fuentes de catálogo geográfico soportadas. Sustituye a los strings literales
 * que antes se usaban en {@code fuente_api} para que el nombre de un proveedor
 * nunca quede disperso por el código.
 */
public enum FuenteCatalogo {

    GEOREF("GEOREF"),
    GEONAMES("GEONAMES"),
    GOOGLEMAPS("GOOGLEMAPS");

    private final String codigo;

    FuenteCatalogo(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Código a persistir en {@code provincia.fuente_api} y {@code ciudad.fuente_api}.
     */
    public String codigo() {
        return codigo;
    }
}
