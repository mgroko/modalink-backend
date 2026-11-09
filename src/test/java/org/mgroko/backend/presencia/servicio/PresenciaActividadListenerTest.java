package org.mgroko.backend.presencia.servicio;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.mgroko.backend.presencia.evento.PresenciaActividadRegistrada;
import org.mockito.Mockito;

class PresenciaActividadListenerTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 9, 12, 0);
    private static final LocalDateTime LIMITE = AHORA.minusSeconds(120);

    @Test
    void primeraActividad_publicaOnlineUnaSolaVez() {
        PresenciaRegistry registry = new PresenciaRegistry();
        PresenciaEventBus eventBus = Mockito.mock(PresenciaEventBus.class);
        PresenciaActividadListener listener =
                new PresenciaActividadListener(registry, eventBus);

        listener.alRegistrarActividad(
                new PresenciaActividadRegistrada(10L, AHORA, LIMITE));
        listener.alRegistrarActividad(
                new PresenciaActividadRegistrada(10L, AHORA.plusSeconds(30), LIMITE));

        verify(eventBus, times(1)).publicar(new PresenciaEvento(10L, true));
        verify(eventBus, never()).publicar(new PresenciaEvento(10L, false));
    }

    @Test
    void actividadDeOtroPerfil_noAfectaAlPrimero() {
        PresenciaRegistry registry = new PresenciaRegistry();
        PresenciaEventBus eventBus = Mockito.mock(PresenciaEventBus.class);
        PresenciaActividadListener listener =
                new PresenciaActividadListener(registry, eventBus);

        listener.alRegistrarActividad(
                new PresenciaActividadRegistrada(10L, AHORA, LIMITE));
        listener.alRegistrarActividad(
                new PresenciaActividadRegistrada(20L, AHORA, LIMITE));

        verify(eventBus).publicar(new PresenciaEvento(10L, true));
        verify(eventBus).publicar(new PresenciaEvento(20L, true));
        verify(eventBus, never()).publicar(new PresenciaEvento(10L, false));
        verify(eventBus, never()).publicar(new PresenciaEvento(20L, false));
    }
}
