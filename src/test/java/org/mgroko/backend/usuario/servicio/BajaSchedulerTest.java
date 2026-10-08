package org.mgroko.backend.usuario.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class BajaSchedulerTest {

    @Mock
    private ExpirarCuentaService expirarCuentaService;

    @Mock
    private ExpirarPerfilService expirarPerfilService;

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @Mock
    private ScheduledTaskRegistrar taskRegistrar;

    @InjectMocks
    private BajaScheduler scheduler;

    @Test
    void configureTasks_registraTriggerTask() {
        scheduler.configureTasks(taskRegistrar);

        verify(taskRegistrar).addTriggerTask(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void triggerTask_alEjecutarse_expiraCuentasYPerfilesConElPlazoConfigurado() {
        when(configuracionSistemaService.obtenerDiasBaja()).thenReturn(45);
        scheduler.configureTasks(taskRegistrar);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskRegistrar).addTriggerTask(captor.capture(), any(Trigger.class));

        Runnable runnable = captor.getValue();
        assertNotNull(runnable);
        runnable.run();

        verify(expirarCuentaService).expirarVencidos(45);
        verify(expirarPerfilService).expirarVencidos(45);
    }

    @Test
    void ejecutarAlInicio_expiraCuentasYPerfilesVencidas() {
        when(configuracionSistemaService.obtenerDiasBaja()).thenReturn(30);

        scheduler.ejecutarAlInicio();

        verify(expirarCuentaService).expirarVencidos(30);
        verify(expirarPerfilService).expirarVencidos(30);
    }

    @Test
    void trigger_alEvaluar_leeElCronConfigurado() {
        when(configuracionSistemaService.obtenerCronBaja()).thenReturn("0 30 4 * * *");

        scheduler.configureTasks(taskRegistrar);

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(taskRegistrar).addTriggerTask(any(Runnable.class), triggerCaptor.capture());

        triggerCaptor.getValue().nextExecution(new SimpleTriggerContext());

        verify(configuracionSistemaService).obtenerCronBaja();
        verify(expirarCuentaService, org.mockito.Mockito.never()).expirarVencidos(anyInt());
    }
}
