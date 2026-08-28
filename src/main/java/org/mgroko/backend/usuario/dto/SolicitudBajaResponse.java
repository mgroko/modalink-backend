package org.mgroko.backend.usuario.dto;

import java.time.LocalDateTime;

public record SolicitudBajaResponse(
        String mensaje,
        LocalDateTime fechaLimite
) {
}
