package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.proyectos.evento.DisponibilidadModificadaEvent;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisponibilidadModificadaListenerTest {

    @Mock
    private JornadaReprogramacionService reprogramacionService;

    @InjectMocks
    private DisponibilidadModificadaListener listener;

    @Test
    void alModificarDisponibilidad_reprogramaAfectados() {
        listener.alModificarDisponibilidad(new DisponibilidadModificadaEvent(
                1L, MotivoReprogramacion.ASIGNACION, 5L));

        verify(reprogramacionService).reprogramarAfectados(
                1L, MotivoReprogramacion.ASIGNACION, 5L);
    }

    @Test
    void alModificarDisponibilidad_sinUsuario_noHaceNada() {
        listener.alModificarDisponibilidad(new DisponibilidadModificadaEvent(
                null, MotivoReprogramacion.ASIGNACION, 5L));

        verify(reprogramacionService, never()).reprogramarAfectados(
                any(), any(), any());
    }

    @Test
    void alModificarDisponibilidad_errorDeReprogramacion_noPropaga() {
        doThrow(new IllegalStateException("boom"))
                .when(reprogramacionService)
                .reprogramarAfectados(eq(1L), any(), any());

        assertThatCode(() -> listener.alModificarDisponibilidad(
                new DisponibilidadModificadaEvent(
                        1L, MotivoReprogramacion.ASIGNACION, 5L)))
                .doesNotThrowAnyException();
    }
}
