package org.mgroko.backend.contratos.dto;

public record ContratoActivoMiembroResponse(
        Long idMiembro,
        Long idTipoAcuerdo,
        Long idContrato,
        String nombre
) {
}
