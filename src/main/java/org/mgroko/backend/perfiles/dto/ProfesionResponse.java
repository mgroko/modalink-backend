package org.mgroko.backend.perfiles.dto;

public record ProfesionResponse(
        Long idProfesion,
        String nombre,
        String descripcion
) {
}