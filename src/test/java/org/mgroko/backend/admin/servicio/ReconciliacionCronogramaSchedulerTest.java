package org.mgroko.backend.admin.servicio;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliacionCronogramaSchedulerTest {

    @Mock
    private ReconciliarCronogramaService reconciliarCronogramaService;

    @Test
    void ejecutar_delegaEnElServicioDeReconciliacion() {
        when(reconciliarCronogramaService.ejecutar())
                .thenReturn(new ReparacionCronogramaResponse(
                        false, 1, 1, 0, List.of(), List.of()));

        ReconciliacionCronogramaScheduler scheduler =
                new ReconciliacionCronogramaScheduler(
                        reconciliarCronogramaService, "-");

        scheduler.ejecutar();

        verify(reconciliarCronogramaService).ejecutar();
    }
}
