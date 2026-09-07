package org.mgroko.backend.calendario.dto;

import java.util.List;

public record ConfigJornadaResponse(
        Integer margenActividadMinutos,
        List<JornadaDiaResponse> dias
) {
}