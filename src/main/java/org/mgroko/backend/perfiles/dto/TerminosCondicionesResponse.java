package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

/**
 * Términos y condiciones vigentes de un perfil, o la plantilla base del
 * sistema cuando el perfil todavía no configuró los suyos (UC-23).
 */
public record TerminosCondicionesResponse(
        Long idPerfil,
        Long idTyc,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean esPlantilla
) {
}
