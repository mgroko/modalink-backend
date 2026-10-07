package org.mgroko.backend.perfiles.dto;

public record ProfesionResponse(
        Long idProfesion,
        String codigo,
        String nombre,
        String descripcion
) {
}