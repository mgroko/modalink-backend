package org.mgroko.backend.usuario.servicio;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que expira las cuentas (UC-07) y los perfiles (UC-12)
 * pendientes de baja una vez transcurrido el plazo de reactivación.
 * <p>
 * Su frecuencia, hora de ejecución y el plazo en días son configurables
 * desde el panel de administrador (paridad con {@code DeshabilitacionScheduler}).
 */
@Slf4j
@Component
public class BajaScheduler implements SchedulingConfigurer {

    private final ExpirarCuentaService expirarCuentaService;
    private final ExpirarPerfilService expirarPerfilService;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public BajaScheduler(
            ExpirarCuentaService expirarCuentaService,
            ExpirarPerfilService expirarPerfilService,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.expirarCuentaService = expirarCuentaService;
        this.expirarPerfilService = expirarPerfilService;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::expirarVencidos,
                triggerContext -> {
                    String cron = configuracionSistemaService.obtenerCronBaja();
                    return new CronTrigger(cron).nextExecution(triggerContext);
                }
        );
    }

    /**
     * Al iniciar la aplicación, expira cuentas y perfiles cuya baja venció
     * mientras el servidor estuvo inactivo o apagado en el horario programado.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlInicio() {
        log.info("Servidor iniciado. Verificando cuentas y perfiles con plazo de baja vencido...");
        expirarVencidos();
    }

    private void expirarVencidos() {
        int diasBaja = configuracionSistemaService.obtenerDiasBaja();
        int cuentas = expirarCuentaService.expirarVencidos(diasBaja);
        int perfiles = expirarPerfilService.expirarVencidos(diasBaja);
        log.info("Expiración de bajas ejecutada. Cuentas: {}, perfiles: {}", cuentas, perfiles);
    }
}
