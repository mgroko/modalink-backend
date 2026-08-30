package org.mgroko.backend.ubicacion.georef;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raíz del archivo {@code resources/georef/localidades.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalidadesGeorefResponse(List<LocalidadGeoref> localidades) {
}