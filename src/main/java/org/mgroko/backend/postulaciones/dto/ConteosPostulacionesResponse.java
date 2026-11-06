package org.mgroko.backend.postulaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Conteos de postulaciones de un proyecto por estado.
 *
 * <p>Se calculan sobre todo el proyecto, ignorando la paginación y el filtro
 * {@code estado}, pero respetando el resto de los filtros de UC-74.</p>
 */
public record ConteosPostulacionesResponse(
        @JsonProperty("PENDIENTE") long pendiente,
        @JsonProperty("ACEPTADA") long aceptada,
        @JsonProperty("RECHAZADA") long rechazada,
        @JsonProperty("TOTAL") long total) {
}
