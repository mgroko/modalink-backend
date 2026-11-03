package org.mgroko.backend.proyectos.dto;

import java.util.List;

/**
 * Actividad fija que quedó fuera de la nueva disponibilidad y no se movió.
 */
public record ConflictoFijoResponse(
        Long idActividad,
        String nombre,
        List<Long> idsUsuarios,
        String mensaje) {

    public ConflictoFijoResponse {
        idsUsuarios = idsUsuarios == null ? List.of() : List.copyOf(idsUsuarios);
    }
}
