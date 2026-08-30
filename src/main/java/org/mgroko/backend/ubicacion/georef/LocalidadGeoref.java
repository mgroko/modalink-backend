package org.mgroko.backend.ubicacion.georef;

/**
 * Localidad del catálogo de Georef (id + nombre + centroide + provincia).
 */
public record LocalidadGeoref(String id, String nombre, CentroideGeoref centroide, ProvinciaGeorefRef provincia) {
}