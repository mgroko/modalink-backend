package org.mgroko.backend.usuario.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que expira las cuentas pendientes de baja (UC-07)
 * una vez transcurridos los 30 días sin que el usuario inicie sesión.
 */
@Component
public class BajaCuentaScheduler {

    private final ExpirarCuentaService expirarCuentaService;

    public BajaCuentaScheduler(ExpirarCuentaService expirarCuentaService) {
        this.expirarCuentaService = expirarCuentaService;
    }

    // Se ejecuta cada día a las 02:00
    @Scheduled(cron = "0 0 2 * * *")
    public void expirarCuentasVencidas() {
        expirarCuentaService.expirarVencidos();
    }
}