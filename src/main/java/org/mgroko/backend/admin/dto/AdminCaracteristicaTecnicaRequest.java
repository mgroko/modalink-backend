package org.mgroko.backend.admin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCaracteristicaTecnicaRequest(
        @NotBlank @Size(max = 50) String codigo,
        @Size(max = 50) String unidad,
        @NotNull Long idProfesion,
        @NotBlank @Size(max = 20) String tipoDato,
        List<@Valid AdminValorCaracteristicaRequest> valores
) {
}