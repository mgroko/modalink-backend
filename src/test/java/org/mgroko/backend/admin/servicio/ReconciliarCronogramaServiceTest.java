package org.mgroko.backend.admin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliarCronogramaServiceTest {

    @Mock
    private RepararCronogramaService repararCronogramaService;

    @Test
    void ejecutar_sinAutoReparar_solicitaDryRun() {
        when(repararCronogramaService.reparar(any()))
                .thenReturn(new ReparacionCronogramaResponse(
                        true, 2, 0, 0, List.of(), List.of()));

        ReconciliarCronogramaService service =
                new ReconciliarCronogramaService(
                        repararCronogramaService, false, 50);

        ReparacionCronogramaResponse response = service.ejecutar();

        assertThat(response.totalDetectadas()).isEqualTo(2);
        assertThat(response.dryRun()).isTrue();

        ArgumentCaptor<org.mgroko.backend.admin.dto.RepararCronogramaRequest> captor =
                ArgumentCaptor.forClass(
                        org.mgroko.backend.admin.dto.RepararCronogramaRequest.class);
        verify(repararCronogramaService).reparar(captor.capture());
        assertThat(captor.getValue().esDryRun()).isTrue();
        assertThat(captor.getValue().limite()).isEqualTo(50);
        assertThat(captor.getValue().idProyecto()).isNull();
        assertThat(captor.getValue().idActividad()).isNull();
    }

    @Test
    void ejecutar_conAutoReparar_aplicaCambios() {
        when(repararCronogramaService.reparar(any()))
                .thenReturn(new ReparacionCronogramaResponse(
                        false, 2, 1, 1, List.of(), List.of("error")));

        ReconciliarCronogramaService service =
                new ReconciliarCronogramaService(
                        repararCronogramaService, true, 10);

        service.ejecutar();

        ArgumentCaptor<org.mgroko.backend.admin.dto.RepararCronogramaRequest> captor =
                ArgumentCaptor.forClass(
                        org.mgroko.backend.admin.dto.RepararCronogramaRequest.class);
        verify(repararCronogramaService).reparar(captor.capture());
        assertThat(captor.getValue().esDryRun()).isFalse();
        assertThat(captor.getValue().limite()).isEqualTo(10);
    }
}
