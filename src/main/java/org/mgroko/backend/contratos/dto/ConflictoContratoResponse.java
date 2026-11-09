package org.mgroko.backend.contratos.dto;

public record ConflictoContratoResponse(
        String codigo,
        Long idMiembro,
        String nombre,
        String mensaje
) {
}
