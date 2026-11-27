package org.mgroko.backend.postulaciones.casting.dto;

public record HabilidadEvaluadaCastingResponse(
        Long idHabilidad,
        String nombre,
        Boolean cumple) {
}
