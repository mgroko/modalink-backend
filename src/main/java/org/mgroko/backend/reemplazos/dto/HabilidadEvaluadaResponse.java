package org.mgroko.backend.reemplazos.dto;

/**
 * Habilidad de un candidato con el resultado de la evaluación contra la
 * vacante.
 *
 * <p>{@code requerida} indica si la vacante pidió la habilidad y
 * {@code cumple} si el candidato la tiene. Cuando la vacante no la pidió,
 * {@code cumple} queda en {@code null} para que la UI la muestre como dato
 * informativo.</p>
 */
public record HabilidadEvaluadaResponse(
        Long idHabilidad,
        String nombre,
        Boolean requerida,
        Boolean cumple) {
}
