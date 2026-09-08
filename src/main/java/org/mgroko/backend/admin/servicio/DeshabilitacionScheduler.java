package org.mgroko.backend.admin.servicio;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que reactiva automáticamente las cuentas cuya
 * deshabilitación con duración (UC-04) ha vencido.
 * <p>
 * Su frecuencia y hora de ejecución son dinámicas y configurables desde el panel de administrador.
 */
@Slf4j
@Component
public class DeshabilitacionScheduler implements SchedulingConfigurer {

    private final ExpirarDeshabilitacionService expirarDeshabilitacionService;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public DeshabilitacionScheduler(
            ExpirarDeshabilitacionService expirarDeshabilitacionService,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.expirarDeshabilitacionService = expirarDeshabilitacionService;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("Ejecutando scheduler programado para reactivar usuarios deshabilitados...");
                    expirarDeshabilitacionService.reactivarVencidos();
                },
                triggerContext -> {
                    String cron = configuracionSistemaService.obtenerCronDeshabilitacion();
                    return new CronTrigger(cron).nextExecution(triggerContext);
                }
        );
    }

    /**
     * Al iniciar la aplicación, reactiva preventivamente usuarios cuya deshabilitación haya vencido
     * mientras el servidor estuvo inactivo o apagado en el horario programado.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlInicio() {
        log.info("Servidor iniciado. Verificando usuarios deshabilitados con vencimiento cumplido...");
        int reactivados = expirarDeshabilitacionService.reactivarVencidos();
        log.info("Verificación de deshabilitados al inicio completada. Reactivados: {}", reactivados);
    }
}