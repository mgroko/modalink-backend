package org.mgroko.backend.mensajeria.servicio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class MensajeriaSchedulerTest {

    @Test
    void ping_delegaEnElBus() {
        MensajeEventBus eventBus = mock(MensajeEventBus.class);

        new MensajeriaScheduler(eventBus).ping();

        verify(eventBus).ping();
    }
}
