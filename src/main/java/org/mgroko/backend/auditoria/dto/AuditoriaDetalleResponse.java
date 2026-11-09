package org.mgroko.backend.auditoria.dto;

public record AuditoriaDetalleResponse(
        String campo,
        String valorAnterior,
        String valorNuevo
) {
}
