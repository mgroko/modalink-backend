package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

/**
 * Resumen de una versión de términos y condiciones de un perfil.
 *
 * <p>{@code vigente} es true cuando la versión no tiene fecha de fin.</p>
 */
public record TerminosCondicionesVersionResumenResponse(
        Long idTyc,
        Integer numeroVersion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean vigente
) {
}
