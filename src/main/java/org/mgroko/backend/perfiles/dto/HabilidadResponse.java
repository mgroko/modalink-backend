package org.mgroko.backend.perfiles.dto;

public record HabilidadResponse(
        Long idHabilidad,
        String codigo,
        String nombre,
        String descripcion
) {
}
