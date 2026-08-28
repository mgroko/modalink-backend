package org.mgroko.backend.perfiles.dto;

import java.util.List;

public record CaracteristicaTecnicaResponse(
        Long idCaracteristica,
        String codigo,
        String unidad,
        Long idProfesion,
        String profesion,
        String tipoDato,
        List<ValorCaracteristicaResponse> valores
) {
}