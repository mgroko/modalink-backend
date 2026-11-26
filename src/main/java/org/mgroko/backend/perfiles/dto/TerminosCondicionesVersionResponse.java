package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

/**
 * Detalle de una versión histórica de términos y condiciones de un perfil.
 */
public record TerminosCondicionesVersionResponse(
        Long idPerfil,
        Long idTyc,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean vigente
) {
}
