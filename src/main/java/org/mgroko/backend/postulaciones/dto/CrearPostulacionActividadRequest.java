package org.mgroko.backend.postulaciones.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de postulación a un requerimiento de actividad (UC-26).
 *
 * @param idRequerimientoActividad requerimiento de la actividad elegida
 */
public record CrearPostulacionActividadRequest(
        @NotNull(message = "El requerimiento de actividad es obligatorio.")
        Long idRequerimientoActividad) {
}
