package org.mgroko.backend.admin.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DeshabilitacionSchedulerTest {

    @Mock
    private ExpirarDeshabilitacionService expirarDeshabilitacionService;

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @Mock
    private ScheduledTaskRegistrar taskRegistrar;

    @InjectMocks
    private DeshabilitacionScheduler scheduler;

    @Test
    void configureTasks_registraTriggerTask() {
        scheduler.configureTasks(taskRegistrar);

        verify(taskRegistrar).addTriggerTask(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void triggerTask_alEjecutarse_invocaReactivarVencidos() {
        scheduler.configureTasks(taskRegistrar);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskRegistrar).addTriggerTask(captor.capture(), any(Trigger.class));

        Runnable runnable = captor.getValue();
        assertNotNull(runnable);
        runnable.run();

        verify(expirarDeshabilitacionService).reactivarVencidos();
    }
}
