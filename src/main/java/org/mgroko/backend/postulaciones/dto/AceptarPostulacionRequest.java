package org.mgroko.backend.postulaciones.dto;

import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;

import jakarta.validation.Valid;

/**
 * UC-34: aceptación de la postulación. Incluye la aceptación de los términos
 * y condiciones vigentes del perfil postulante, si tiene configurados.
 */
public record AceptarPostulacionRequest(
        @Valid AceptacionTerminosRequest aceptacionTerminos
) {
}
