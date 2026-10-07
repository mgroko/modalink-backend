package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El intervalo de la limpieza se lee de la configuraci&oacute;n en cada ciclo,
 * as&iacute; que un cambio en {@code IMAGEN_LIMPIEZA_INTERVALO_HORAS} se aplica sin
 * reiniciar el servidor.
 */
@ExtendWith(MockitoExtension.class)
class LimpiezaImagenesSchedulerTest {

    @Mock
    private LimpiezaImagenesService limpiezaImagenesService;

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @InjectMocks
    private LimpiezaImagenesScheduler limpiezaImagenesScheduler;

    @Test
    @DisplayName("configureTasks - Registra una única tarea con su trigger")
    void configureTasks_registraLaTareaDeLimpieza() {
        ScheduledTaskRegistrar registro = new ScheduledTaskRegistrar();

        limpiezaImagenesScheduler.configureTasks(registro);

        List<TriggerTask> tareas = registro.getTriggerTaskList();
        assertEquals(1, tareas.size());
        assertNotNull(tareas.get(0).getTrigger());
        assertNotNull(tareas.get(0).getRunnable());
    }

    @Test
    @DisplayName("La tarea programada - Ejecuta la limpieza y registra el resultado")
    void tareaProgramada_ejecutaLaLimpieza() {
        when(limpiezaImagenesService.ejecutar())
                .thenReturn(new LimpiezaImagenesService.Resultado(2, 3, 1, 0));
        ScheduledTaskRegistrar registro = new ScheduledTaskRegistrar();
        limpiezaImagenesScheduler.configureTasks(registro);

        registro.getTriggerTaskList().get(0).getRunnable().run();

        verify(limpiezaImagenesService).ejecutar();
    }

    @Test
    @DisplayName("triggerLimpieza - Refleja el cambio de intervalo sin reiniciar")
    void triggerLimpieza_reflejaElCambioDeIntervaloSinReiniciar() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        TriggerContext contexto = mock(TriggerContext.class);
        when(contexto.lastScheduledExecution()).thenReturn(base);
        when(configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras()).thenReturn(24, 6);

        Trigger trigger = limpiezaImagenesScheduler.triggerLimpieza();

        assertEquals(base.plus(Duration.ofHours(24)), trigger.nextExecution(contexto),
                "El primer ciclo usa el intervalo configurado");
        assertEquals(base.plus(Duration.ofHours(6)), trigger.nextExecution(contexto),
                "Un cambio de configuración debe notarse en el siguiente cálculo");
    }

    @Test
    @DisplayName("triggerLimpieza - Sin ejecución previa parte de ahora")
    void triggerLimpieza_sinEjecucionPrevia_parteDeAhora() {
        TriggerContext contexto = mock(TriggerContext.class);
        when(contexto.lastScheduledExecution()).thenReturn(null);
        when(configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras()).thenReturn(1);

        Instant antes = Instant.now();
        Instant proxima = limpiezaImagenesScheduler.triggerLimpieza().nextExecution(contexto);

        assertTrue(!proxima.isBefore(antes.plus(Duration.ofHours(1)).minusSeconds(5)),
                "Debe programarse a partir de ahora: " + proxima);
        assertTrue(proxima.isBefore(antes.plus(Duration.ofHours(1)).plusSeconds(5)),
                "El intervalo configurado debe respetarse: " + proxima);
    }
}
