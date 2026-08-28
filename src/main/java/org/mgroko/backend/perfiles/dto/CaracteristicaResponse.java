package org.mgroko.backend.perfiles.dto;

public record CaracteristicaResponse(
        Long idCaracteristica,
        String codigo,
        String valor,
        Long idValor,
        String codigoValor,
        String colorHex
) {
}