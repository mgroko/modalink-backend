package org.mgroko.backend.reemplazos.servicio.modelo;

/**
 * Resultado de evaluar una característica pedida por la vacante.
 */
public record CaracteristicaEvaluada(
        Long idCaracteristica,
        String codigo,
        String nombre,
        boolean requerida,
        Boolean cumple,
        String valorPedido) {
}
