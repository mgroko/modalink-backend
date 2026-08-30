package org.mgroko.backend.perfiles.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que expira los perfiles pendientes de baja (UC-12)
 * una vez transcurridos los 30 días sin reactivación.
 */
@Component
public class BajaPerfilScheduler {

    private final ExpirarPerfilService expirarPerfilService;

    public BajaPerfilScheduler(ExpirarPerfilService expirarPerfilService) {
        this.expirarPerfilService = expirarPerfilService;
    }

    // Se ejecuta cada día a las 02:00
    @Scheduled(cron = "0 0 2 * * *")
    public void expirarPerfilesVencidos() {
        expirarPerfilService.expirarVencidos();
    }
}