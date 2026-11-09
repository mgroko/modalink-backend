package org.mgroko.backend.auditoria.dto;

public record AuditoriaResumenTipoResponse(
        String codigo,
        String nombre,
        long total
) {
}
