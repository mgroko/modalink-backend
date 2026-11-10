package org.mgroko.backend.presencia.servicio;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresenciaSchedulerTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 9, 12, 0);
    private static final LocalDateTime LIMITE = AHORA.minusSeconds(120);

    @Mock
    private PresenciaRegistry registry;

    @Mock
    private PresenciaEventBus eventBus;

    @Mock
    private PresenciaService presenciaService;

    @Test
    void expirar_publicaOfflineParaLosVencidos() {
        when(presenciaService.ahora()).thenReturn(AHORA);
        when(presenciaService.limite(AHORA)).thenReturn(LIMITE);
        when(registry.expirar(LIMITE)).thenReturn(List.of(10L, 20L));

        new PresenciaScheduler(registry, eventBus, presenciaService).expirar();

        verify(eventBus).publicar(new PresenciaEvento(10L, false));
        verify(eventBus).publicar(new PresenciaEvento(20L, false));
    }

    @Test
    void ping_delegaEnElBus() {
        new PresenciaScheduler(registry, eventBus, presenciaService).ping();

        verify(eventBus).ping();
    }
}
