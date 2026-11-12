package org.mgroko.backend.storage.servicio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.storage.exception.LimpiezaImagenesEnCursoException;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Programa la limpieza de im&aacute;genes hu&eacute;rfanas (Fase 5).
 * <p>
 * La frecuencia no puede ser una expresi&oacute;n fija: el intervalo se lee de
 * {@code IMAGEN_LIMPIEZA_INTERVALO_HORAS} en <b>cada</b> ciclo, por lo que un
 * cambio en {@code configuracion_sistema} se aplica sin reiniciar el servidor
 * ({@code @Scheduled} no puede leer la BD).
 */
@Slf4j
@Component
public class LimpiezaImagenesScheduler implements SchedulingConfigurer {

    private final LimpiezaImagenesService limpiezaImagenesService;
    private final ConfiguracionSistemaService configuracionSistemaService;

    private volatile LocalDateTime ultimaEjecucion;
    private volatile LocalDateTime ultimaEjecucionProgramada;

    public LimpiezaImagenesScheduler(
            LimpiezaImagenesService limpiezaImagenesService,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.limpiezaImagenesService = limpiezaImagenesService;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::limpiar, triggerLimpieza());
    }

    /**
     * Trigger con intervalo din&aacute;mico: consulta la configuraci&oacute;n en cada
     * ciclo y programa la pr&oacute;xima ejecuci&oacute;n a tantas horas como indique.
     *
     * @return trigger de la tarea de limpieza
     */
    public Trigger triggerLimpieza() {
        return triggerContext -> {
            int horas = configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras();
            Instant ultima = triggerContext.lastScheduledExecution();
            Instant base = ultima != null ? ultima : Instant.now();
            return base.plus(Duration.ofHours(horas));
        };
    }

    /**
     * Próxima ejecución programada según el intervalo configurado. Si todavía
     * no hubo una ejecución programada, se calcula desde ahora.
     *
     * @return fecha y hora estimada de la próxima ejecución
     */
    public LocalDateTime proximaEjecucion() {
        int horas = configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime base = ultimaEjecucionProgramada != null ? ultimaEjecucionProgramada : ahora;
        LocalDateTime proxima = base.plusHours(horas);
        return proxima.isBefore(ahora) ? ahora : proxima;
    }

    /**
     * Última ejecución, programada o manual.
     *
     * @return fecha y hora de la última ejecución, o {@code null}
     */
    public LocalDateTime ultimaEjecucion() {
        return ultimaEjecucion;
    }

    /**
     * Registra una ejecución manual para que el panel de administración pueda
     * mostrarla. No altera el cálculo de la próxima ejecución programada.
     */
    public void marcarEjecucionManual() {
        this.ultimaEjecucion = LocalDateTime.now();
    }

    private void limpiar() {
        try {
            LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();
            LocalDateTime ahora = LocalDateTime.now();
            this.ultimaEjecucion = ahora;
            this.ultimaEjecucionProgramada = ahora;
            log.info("Limpieza de imágenes programada: {} archivo(s) huérfano(s), "
                            + "{} imagen(es) sin referencia, {} imagen(es) de perfil en baja, {} error(es).",
                    resultado.archivosHuerfanos(),
                    resultado.imagenesHuerfanas(),
                    resultado.imagenesPerfilesBaja(),
                    resultado.errores());
        } catch (LimpiezaImagenesEnCursoException e) {
            log.warn("Se omitió la limpieza programada porque ya hay una ejecución en curso.");
        }
    }
}
