package org.mgroko.backend.postulaciones.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de postulación a un requerimiento general de proyecto (UC-26).
 *
 * @param idRequerimientoGral requerimiento general elegido
 */
public record CrearPostulacionGralRequest(
        @NotNull(message = "El requerimiento general es obligatorio.")
        Long idRequerimientoGral) {
}
