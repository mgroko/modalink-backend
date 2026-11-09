package org.mgroko.backend.reemplazos.dto;

import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;

import jakarta.validation.Valid;

/**
 * UC-38 bis: aprobación de un candidato de reemplazo. Incluye la aceptación
 * de los términos y condiciones vigentes del perfil del candidato, si tiene
 * configurados.
 */
public record AprobarCandidatoRequest(
        @Valid AceptacionTerminosRequest aceptacionTerminos
) {
}
