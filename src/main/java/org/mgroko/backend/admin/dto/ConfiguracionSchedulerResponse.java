package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

public record ConfiguracionSchedulerResponse(
        Integer hora,
        Integer minuto,
        String cron,
        LocalDateTime proximaEjecucion
) {
}
