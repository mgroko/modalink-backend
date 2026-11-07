package org.mgroko.backend.contratos.dto;

public record ParteContratoResponse(
        Long idMiembro,
        Long idPerfil,
        String nombre,
        String rolParte,
        String estadoFirma
) {
}
