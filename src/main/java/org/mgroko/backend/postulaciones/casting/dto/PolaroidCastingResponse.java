package org.mgroko.backend.postulaciones.casting.dto;

import java.time.LocalDateTime;

public record PolaroidCastingResponse(
        Long idPolaroid,
        Long idImagen,
        String url,
        Integer orden,
        LocalDateTime fechaRegistro,
        int prioridadCasting,
        TipoPolaroidCastingResponse tipo) {
}
