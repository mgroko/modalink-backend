package org.mgroko.backend.actividades.dto;

import org.mgroko.backend.actividades.enums.AccionIntegrante;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Decisión del director sobre un miembro exclusivo de la actividad eliminada.
 *
 * @param idMiembro  miembro de proyecto
 * @param accion     eliminar del proyecto o conservarlo
 * @param motivoBaja motivo opcional de la baja; si falta se usa el motivo de
 *                   la eliminación de la actividad
 */
public record DecisionIntegranteRequest(
        @NotNull(message = "El miembro es obligatorio.")
        @Positive(message = "El miembro debe ser válido.")
        Long idMiembro,

        @NotNull(message = "La acción sobre el integrante es obligatoria.")
        AccionIntegrante accion,

        @Size(max = 100, message = "El motivo de baja no puede superar los 100 caracteres.")
        String motivoBaja
) {
}
