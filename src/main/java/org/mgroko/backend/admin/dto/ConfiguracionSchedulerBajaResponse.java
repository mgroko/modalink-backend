package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

public record ConfiguracionSchedulerBajaResponse(
        Integer hora,
        Integer minuto,
        String cron,
        LocalDateTime proximaEjecucion,
        Integer diasBaja
) {
}
