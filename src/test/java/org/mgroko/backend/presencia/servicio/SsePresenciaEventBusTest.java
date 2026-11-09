package org.mgroko.backend.presencia.servicio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SsePresenciaEventBusTest {

    private final SsePresenciaEventBus bus = new SsePresenciaEventBus();

    @Test
    void publicar_enviaSoloALosSuscriptoresDelPerfil() throws Exception {
        SseEmitter suscriptorA = mock(SseEmitter.class);
        SseEmitter suscriptorB = mock(SseEmitter.class);

        bus.registrarSuscriptor(List.of(1L, 2L), suscriptorA);
        bus.registrarSuscriptor(List.of(2L), suscriptorB);

        bus.publicar(new PresenciaEvento(1L, true));

        verify(suscriptorA, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(suscriptorB, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void ping_enviaATodosLosSuscriptores() throws Exception {
        SseEmitter suscriptorA = mock(SseEmitter.class);
        SseEmitter suscriptorB = mock(SseEmitter.class);

        bus.registrarSuscriptor(List.of(1L), suscriptorA);
        bus.registrarSuscriptor(List.of(2L), suscriptorB);

        bus.ping();

        verify(suscriptorA).send(any(SseEmitter.SseEventBuilder.class));
        verify(suscriptorB).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void eliminarSuscriptor_dejaDeRecibirEventos() throws Exception {
        SseEmitter suscriptor = mock(SseEmitter.class);
        bus.registrarSuscriptor(List.of(1L), suscriptor);

        bus.eliminarSuscriptor(suscriptor);
        bus.publicar(new PresenciaEvento(1L, false));

        verify(suscriptor, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
