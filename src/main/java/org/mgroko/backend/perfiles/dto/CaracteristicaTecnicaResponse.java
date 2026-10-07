package org.mgroko.backend.perfiles.dto;

import java.util.List;

import org.mgroko.backend.admin.dto.UnidadMedidaResponse;

public record CaracteristicaTecnicaResponse(
        Long idCaracteristica,
        String codigo,
        String nombre,
        UnidadMedidaResponse unidad,
        Long idProfesion,
        String profesion,
        String tipoDato,
        List<ValorCaracteristicaResponse> valores
) {
}