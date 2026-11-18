package org.mgroko.backend.admin.servicio;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcesamientoHistoricoWorkerTest {

    @Mock
    private ProcesamientoHistoricoBatchService batchService;

    private ProcesamientoHistoricoWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ProcesamientoHistoricoWorker(batchService);
    }

    @Test
    void ejecutar_procesaLotesYFinaliza() {
        when(batchService.iniciar(5L)).thenReturn(jobEnProgreso());
        when(batchService.estaCancelado(5L)).thenReturn(false);
        when(batchService.siguienteLote(5L))
                .thenReturn(List.of(10L, 20L), List.of());

        worker.ejecutar(5L);

        verify(batchService).procesarPublicacion(5L, 10L);
        verify(batchService).procesarPublicacion(5L, 20L);
        verify(batchService).finalizar(5L);
    }

    @Test
    void ejecutar_siEstaCancelado_noPideLote() {
        when(batchService.iniciar(5L)).thenReturn(jobEnProgreso());
        when(batchService.estaCancelado(5L)).thenReturn(true);

        worker.ejecutar(5L);

        verify(batchService, never()).siguienteLote(5L);
        verify(batchService).finalizar(5L);
    }

    @Test
    void ejecutar_errorEnUnaPublicacion_registraYContinua() {
        when(batchService.iniciar(5L)).thenReturn(jobEnProgreso());
        when(batchService.estaCancelado(5L)).thenReturn(false);
        when(batchService.siguienteLote(5L))
                .thenReturn(List.of(10L), List.of());
        doThrow(new IllegalStateException("boom"))
                .when(batchService).procesarPublicacion(5L, 10L);

        worker.ejecutar(5L);

        verify(batchService).registrarError(5L, 10L, "boom");
        verify(batchService).finalizar(5L);
    }

    @Test
    void ejecutar_errorFatal_marcaFallido() {
        when(batchService.iniciar(5L))
                .thenThrow(new IllegalStateException("fatal"));

        worker.ejecutar(5L);

        verify(batchService).fallar(5L, "fatal");
    }

    private ProcesamientoColaboracionHistorica jobEnProgreso() {
        return ProcesamientoColaboracionHistorica.builder()
                .idProcesamiento(5L)
                .estado(EstadoProcesamientoHistorico.EN_PROGRESO)
                .modo(ModoProcesamientoHistorico.AMBOS)
                .notificar(false)
                .tamanoLote(50)
                .idUsuarioAdmin(1L)
                .fechaInicio(LocalDateTime.now())
                .build();
    }
}
