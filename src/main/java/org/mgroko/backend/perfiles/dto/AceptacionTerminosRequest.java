package org.mgroko.backend.perfiles.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aceptación explícita de una versión vigente de los términos y condiciones
 * de un perfil (UC-31, UC-34 y UC-38 bis).
 */
public record AceptacionTerminosRequest(
        @NotNull(message = "La versión de los términos y condiciones es obligatoria.")
        Long idTyc,

        @NotNull(message = "Debe confirmar la aceptación de los términos y condiciones.")
        Boolean aceptaTerminos
) {
}
