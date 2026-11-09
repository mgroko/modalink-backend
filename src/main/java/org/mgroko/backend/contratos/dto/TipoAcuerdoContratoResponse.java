package org.mgroko.backend.contratos.dto;

public record TipoAcuerdoContratoResponse(
        Long idTipoAcuerdo,
        String codigo,
        String nombre,
        String descripcion
) {
}
