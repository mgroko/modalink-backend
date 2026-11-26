package org.mgroko.backend.postulaciones.casting.dto;

public record CaracteristicaPerfilCastingResponse(
        Long idCaracteristica,
        String codigo,
        String nombre,
        String tipoDato,
        String valor,
        Long idValor,
        String etiqueta,
        String colorHex,
        String unidad) {
}
