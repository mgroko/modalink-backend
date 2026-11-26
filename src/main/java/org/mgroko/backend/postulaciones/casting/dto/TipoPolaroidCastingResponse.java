package org.mgroko.backend.postulaciones.casting.dto;

public record TipoPolaroidCastingResponse(
        Long idTipoPolaroid,
        String codigo,
        String nombre,
        String grupoCodigo,
        String grupoNombre,
        String nombreCompleto) {
}
