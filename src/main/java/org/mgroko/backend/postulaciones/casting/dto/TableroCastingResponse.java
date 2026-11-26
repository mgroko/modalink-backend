package org.mgroko.backend.postulaciones.casting.dto;

import java.util.List;

import org.mgroko.backend.postulaciones.dto.ConteosPostulacionesResponse;

public record TableroCastingResponse(
        ProyectoCastingResponse proyecto,
        RequerimientoCastingResponse requerimiento,
        List<CandidatoCastingResponse> contenido,
        int paginaActual,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima,
        ConteosPostulacionesResponse conteosPorEstado) {

    public TableroCastingResponse {
        contenido = contenido == null ? List.of() : List.copyOf(contenido);
    }
}
