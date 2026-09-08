package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

public record EjecutarSchedulerResponse(
        int registrosAfectados,
        LocalDateTime ejecutadoEn,
        String mensaje
) {
}
