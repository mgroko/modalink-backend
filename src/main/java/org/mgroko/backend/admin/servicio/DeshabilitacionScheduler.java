package org.mgroko.backend.admin.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que reactiva automáticamente las cuentas cuya
 * deshabilitación con duración (UC-04) ha vencido.
 */
@Component
public class DeshabilitacionScheduler {

    private final ExpirarDeshabilitacionService expirarDeshabilitacionService;

    public DeshabilitacionScheduler(ExpirarDeshabilitacionService expirarDeshabilitacionService) {
        this.expirarDeshabilitacionService = expirarDeshabilitacionService;
    }

    // Se ejecuta cada día a las 02:00
    @Scheduled(cron = "0 0 2 * * *")
    public void reactivarDeshabilitacionesVencidas() {
        expirarDeshabilitacionService.reactivarVencidos();
    }
}