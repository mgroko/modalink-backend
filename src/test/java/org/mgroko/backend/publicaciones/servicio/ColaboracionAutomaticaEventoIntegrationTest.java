package org.mgroko.backend.publicaciones.servicio;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.publicaciones.evento.ColaboracionAutomaticaSolicitadaEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ColaboracionAutomaticaEventoIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private DetectarColaboracionAutomaticaAsyncService asyncService;

    @Test
    void elEventoSeProcesaDespuesDelCommit() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(
                    new ColaboracionAutomaticaSolicitadaEvent(99L, List.of(10L)));

            // Todavía no se ejecutó: el listener espera el commit.
            verify(asyncService, never()).procesar(99L, List.of(10L));
        });

        verify(asyncService).procesar(99L, List.of(10L));
    }

    @Test
    void elEventoNoSeProcesaSiLaTransaccionHaceRollback() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(
                    new ColaboracionAutomaticaSolicitadaEvent(99L, List.of(10L)));
            status.setRollbackOnly();
        });

        verify(asyncService, never()).procesar(99L, List.of(10L));
    }
}
