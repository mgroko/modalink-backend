package org.mgroko.backend.calendario.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.ResumenReprogramacionResponse;

public record ConfigJornadaResponse(
        Integer margenActividadMinutos,
        List<JornadaDiaResponse> dias,
        ResumenReprogramacionResponse reprogramacion
) {

    /**
     * Constructor histórico: las lecturas de jornada no incluyen el resumen de
     * reprogramación, que solo aparece al guardar.
     */
    public ConfigJornadaResponse(
            Integer margenActividadMinutos,
            List<JornadaDiaResponse> dias) {
        this(margenActividadMinutos, dias, null);
    }
}