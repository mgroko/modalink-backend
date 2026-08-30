package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;

public class ValorCaracteristicaMapper {

    private ValorCaracteristicaMapper() {}

    public static ValorCaracteristicaResponse toResponse(ValorCaracteristica valor) {
        return new ValorCaracteristicaResponse(
                valor.getIdValor(),
                valor.getCodigo(),
                valor.getColorHex());
    }
}