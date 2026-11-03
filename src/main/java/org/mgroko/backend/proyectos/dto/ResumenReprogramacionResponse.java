package org.mgroko.backend.proyectos.dto;

import java.util.List;

/**
 * Resumen de la reprogramación automática disparada por un cambio de jornada
 * o margen.
 */
public record ResumenReprogramacionResponse(
        int totalProyectos,
        List<ReprogramacionProyectoResponse> exitosos,
        List<ReprogramacionFallidaResponse> fallidos) {

    public ResumenReprogramacionResponse {
        exitosos = exitosos == null ? List.of() : List.copyOf(exitosos);
        fallidos = fallidos == null ? List.of() : List.copyOf(fallidos);
    }

    public static ResumenReprogramacionResponse vacio() {
        return new ResumenReprogramacionResponse(0, List.of(), List.of());
    }

    public boolean hayFallidos() {
        return !fallidos.isEmpty();
    }
}
