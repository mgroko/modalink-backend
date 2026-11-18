package org.mgroko.backend.reemplazos.dto;

/**
 * Característica de un candidato enriquecida con el requisito de la vacante.
 *
 * <p>Conserva los campos que ya exponía el detalle del candidato y agrega el
 * valor pedido por la vacante y el resultado de la comparación. Cuando la
 * característica no forma parte del requisito, {@code requerida} es
 * {@code false} y {@code cumple} es {@code null}.</p>
 */
public record CaracteristicaEvaluadaResponse(
        Long idCaracteristica,
        String codigo,
        String nombre,
        String valor,
        Long idValor,
        String codigoValor,
        String colorHex,
        String valorPedido,
        Boolean requerida,
        Boolean cumple) {
}
