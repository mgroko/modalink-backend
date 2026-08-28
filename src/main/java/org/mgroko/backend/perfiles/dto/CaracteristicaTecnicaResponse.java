package org.mgroko.backend.perfiles.dto;

public record CaracteristicaTecnicaResponse(
        Long idCaracteristica,
        String codigo,
        String unidad,
        Long idProfesion,
        String profesion
) {
}