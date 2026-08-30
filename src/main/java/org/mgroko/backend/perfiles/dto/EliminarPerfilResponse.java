package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;

public record EliminarPerfilResponse(
        String mensaje,
        LocalDateTime fechaLimite
) {
}