package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContratoGeneradoResponse(
        Long idContrato,
        Long idProyecto,
        String nombre,
        String descripcion,
        String estado,
        Integer versionActual,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEmision,
        TipoAcuerdoContratoResponse tipoAcuerdo,
        String texto,
        List<ParteContratoResponse> partes
) {
}
