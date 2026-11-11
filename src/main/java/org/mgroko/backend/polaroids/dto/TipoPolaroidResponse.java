package org.mgroko.backend.polaroids.dto;

public record TipoPolaroidResponse(
        Long idTipoPolaroid,
        String codigo,
        String nombre,
        String nombreCompleto
) {
}
