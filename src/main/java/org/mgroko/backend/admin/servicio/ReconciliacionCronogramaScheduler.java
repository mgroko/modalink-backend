package org.mgroko.backend.admin.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Job periódico de reconciliación de cronogramas.
 *
 * <p>Está deshabilitado por defecto: {@code agenda.reconciliacion-cronograma.cron}
 * debe configurarse con una expresión cron para activarlo. El job detecta
 * inconsistencias en proyectos publicados o confirmados y, si
 * {@code agenda.reconciliacion-cronograma.auto-reparar=true}, mueve las
 * actividades pendientes que puedan repararse.</p>
 */
@Slf4j
@Component
public class ReconciliacionCronogramaScheduler implements SchedulingConfigurer {

    private static final String CRON_DESHABILITADO = "-";

    private final ReconciliarCronogramaService reconciliarCronogramaService;
    private final String cron;

    public ReconciliacionCronogramaScheduler(
            ReconciliarCronogramaService reconciliarCronogramaService,
            @Value("${agenda.reconciliacion-cronograma.cron:-}") String cron) {
        this.reconciliarCronogramaService = reconciliarCronogramaService;
        this.cron = cron;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (cron == null
                || cron.isBlank()
                || CRON_DESHABILITADO.equals(cron.trim())) {
            log.info("Reconciliación periódica de cronogramas deshabilitada.");
            return;
        }

        taskRegistrar.addTriggerTask(
                this::ejecutar,
                triggerContext -> new CronTrigger(cron.trim())
                        .nextExecution(triggerContext));
    }

    void ejecutar() {
        var resultado = reconciliarCronogramaService.ejecutar();
        log.info("Reconciliación de cronogramas: {} detectada(s), "
                        + "{} reparada(s), {} fallida(s).",
                resultado.totalDetectadas(),
                resultado.totalReparadas(),
                resultado.totalFallidas());

        if (!resultado.errores().isEmpty()) {
            log.warn("Reconciliación de cronogramas con {} error(es): {}",
                    resultado.errores().size(),
                    resultado.errores());
        }
    }
}
