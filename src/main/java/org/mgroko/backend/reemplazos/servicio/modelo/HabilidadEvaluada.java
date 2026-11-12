package org.mgroko.backend.reemplazos.servicio.modelo;

/**
 * Resultado de evaluar una habilidad pedida por la vacante.
 */
public record HabilidadEvaluada(
        Long idHabilidad,
        String nombre,
        boolean requerida,
        Boolean cumple) {
}
