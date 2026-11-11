package org.mgroko.backend.polaroids.dto;

import java.time.LocalDateTime;

public record PolaroidResponse(
        Long idPolaroid,
        Long idImagen,
        String url,
        Integer orden,
        LocalDateTime fechaRegistro,
        TipoPolaroidResponse tipo
) {
}
