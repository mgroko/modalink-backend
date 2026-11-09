package org.mgroko.backend.mensajeria.servicio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.mgroko.backend.mensajeria.dto.MensajeStreamResponse;

class MensajeEventoListenerTest {

    @Test
    void mensaje_delegaEnElBus() {
        MensajeEventBus eventBus = mock(MensajeEventBus.class);
        MensajeEventoListener listener = new MensajeEventoListener(eventBus);

        MensajeEvento evento = new MensajeEvento(
                30L, 10L, 20L,
                new MensajeStreamResponse(
                        50L, 30L, "Hola",
                        LocalDateTime.of(2026, 11, 9, 15, 0),
                        10L, null, false, null));

        listener.alEnviarMensaje(evento);

        verify(eventBus).publicarMensaje(evento);
    }

    @Test
    void leido_delegaEnElBus() {
        MensajeEventBus eventBus = mock(MensajeEventBus.class);
        MensajeEventoListener listener = new MensajeEventoListener(eventBus);

        ConversacionLeidaEvento evento = new ConversacionLeidaEvento(
                30L, 20L, 10L, 50L,
                LocalDateTime.of(2026, 11, 9, 15, 5));

        listener.alLeerConversacion(evento);

        verify(eventBus).publicarLeido(evento);
    }
}
