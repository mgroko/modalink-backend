package org.mgroko.backend.admin.dto;

public record UnidadMedidaResponse(
        Long idUnidad,
        String nombre,
        String simbolo,
        String tipoDatoPermitido
) {
}
