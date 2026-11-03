package org.mgroko.backend.proyectos.dto;

import java.util.List;

/**
 * Proyecto que no pudo reprogramarse. Su plan anterior queda intacto.
 */
public record ReprogramacionFallidaResponse(
        Long idProyecto,
        String nombreProyecto,
        String codigo,
        String mensaje,
        List<BloqueoConfirmacionResponse> bloqueos) {

    public ReprogramacionFallidaResponse {
        bloqueos = bloqueos == null ? List.of() : List.copyOf(bloqueos);
    }
}
