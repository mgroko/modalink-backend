package org.mgroko.backend.storage.servicio;

import java.time.Duration;
import java.time.Instant;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
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

    private void limpiar() {
        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();
        log.info("Limpieza de imágenes programada: {} archivo(s) huérfano(s), "
                        + "{} imagen(es) sin referencia, {} imagen(es) de perfil en baja, {} error(es).",
                resultado.archivosHuerfanos(),
                resultado.imagenesHuerfanas(),
                resultado.imagenesPerfilesBaja(),
                resultado.errores());
    }
}
