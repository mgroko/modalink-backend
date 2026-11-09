package org.mgroko.backend.mensajeria.servicio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.EscribiendoEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalEvento;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalStreamResponse;
import org.mgroko.backend.mensajeria.dto.MensajeStreamResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseMensajeEventBusTest {

    private final SseMensajeEventBus bus = new SseMensajeEventBus();

    @Test
    void publicarMensaje_enviaAlEmisorYAlReceptor() throws Exception {
        SseEmitter emisor = mock(SseEmitter.class);
        SseEmitter receptor = mock(SseEmitter.class);
        SseEmitter ajeno = mock(SseEmitter.class);

        bus.registrarSuscriptor(10L, emisor);
        bus.registrarSuscriptor(20L, receptor);
        bus.registrarSuscriptor(30L, ajeno);

        bus.publicarMensaje(evento());

        verify(emisor).send(any(SseEmitter.SseEventBuilder.class));
        verify(receptor).send(any(SseEmitter.SseEventBuilder.class));
        verify(ajeno, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publicarMensajeGrupal_enviaAEmisorYATodosLosParticipantesSinDuplicar() throws Exception {
        SseEmitter emisor = mock(SseEmitter.class);
        SseEmitter participante = mock(SseEmitter.class);
        SseEmitter otroParticipante = mock(SseEmitter.class);
        SseEmitter ajeno = mock(SseEmitter.class);

        bus.registrarSuscriptor(10L, emisor);
        bus.registrarSuscriptor(20L, participante);
        bus.registrarSuscriptor(30L, otroParticipante);
        bus.registrarSuscriptor(40L, ajeno);

        bus.publicarMensajeGrupal(new MensajeGrupalEvento(
                50L,
                10L,
                List.of(10L, 20L, 30L, 20L),
                new MensajeGrupalStreamResponse(
                        99L,
                        50L,
                        "Hola",
                        LocalDateTime.of(2026, 11, 9, 15, 0),
                        10L,
                        null)));

        verify(emisor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(participante, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(otroParticipante, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(ajeno, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publicarLeido_enviaAEmisorYReceptor() throws Exception {
        SseEmitter emisor = mock(SseEmitter.class);
        SseEmitter receptor = mock(SseEmitter.class);

        bus.registrarSuscriptor(10L, emisor);
        bus.registrarSuscriptor(20L, receptor);

        bus.publicarLeido(new ConversacionLeidaEvento(
                30L, 20L, 10L, 50L, LocalDateTime.of(2026, 11, 9, 15, 0)));

        verify(emisor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(receptor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void eliminarSuscriptor_dejaDeRecibirEventos() throws Exception {
        SseEmitter emisor = mock(SseEmitter.class);
        bus.registrarSuscriptor(10L, emisor);

        bus.eliminarSuscriptor(emisor);
        bus.publicarMensaje(evento());

        verify(emisor, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publicarEscribiendo_enviaSoloAlDestinatario() throws Exception {
        SseEmitter emisor = mock(SseEmitter.class);
        SseEmitter receptor = mock(SseEmitter.class);
        SseEmitter ajeno = mock(SseEmitter.class);

        bus.registrarSuscriptor(10L, emisor);
        bus.registrarSuscriptor(20L, receptor);
        bus.registrarSuscriptor(30L, ajeno);

        bus.publicarEscribiendo(new EscribiendoEvento(
                30L, 10L, 20L, true,
                LocalDateTime.of(2026, 11, 9, 15, 0)));

        verify(receptor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emisor, never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(ajeno, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    private MensajeEvento evento() {
        return new MensajeEvento(
                30L,
                10L,
                20L,
                new MensajeStreamResponse(
                        50L, 30L, "Hola",
                        LocalDateTime.of(2026, 11, 9, 15, 0),
                        10L, null, false, null));
    }
}
