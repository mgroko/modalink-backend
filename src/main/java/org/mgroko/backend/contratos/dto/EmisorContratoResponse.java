package org.mgroko.backend.contratos.dto;

public record EmisorContratoResponse(
        Long idMiembro,
        Long idPerfil,
        String nombreArtistico,
        String nombreCompleto,
        String dni
) {
}
