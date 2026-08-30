package org.mgroko.backend.ubicacion.georef;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raíz del archivo {@code resources/georef/provincias.json}.
 * Ignora los metadatos extra que devuelve la API de Georef (cantidad,
 * inicio, parametros).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvinciasGeorefResponse(List<ProvinciaGeoref> provincias) {
}