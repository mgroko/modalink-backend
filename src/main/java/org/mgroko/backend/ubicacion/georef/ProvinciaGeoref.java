package org.mgroko.backend.ubicacion.georef;

/**
 * Provincia del catálogo de Georef (id + nombre + centroide).
 */
public record ProvinciaGeoref(String id, String nombre, CentroideGeoref centroide) {
}