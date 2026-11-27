package org.mgroko.backend.postulaciones.casting.dto;

public record CaracteristicaEvaluadaCastingResponse(
        Long idCaracteristica,
        String codigo,
        String nombre,
        boolean requerida,
        Boolean cumple,
        String valorPedido,
        String valorPerfil,
        String unidad) {
}
