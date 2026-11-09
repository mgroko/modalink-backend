package org.mgroko.backend.auditoria.dto;

public record TipoEventoAuditoriaResponse(
        String codigo,
        String nombre,
        String descripcion,
        String modulo
) {
}
