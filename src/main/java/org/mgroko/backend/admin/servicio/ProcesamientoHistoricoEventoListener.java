package org.mgroko.backend.admin.servicio;

import org.mgroko.backend.admin.evento.ProcesamientoHistoricoSolicitadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Encola el procesamiento histórico después del commit del job.
 */
@Component
public class ProcesamientoHistoricoEventoListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProcesamientoHistoricoEventoListener.class);

    private final ProcesamientoHistoricoWorker worker;

    public ProcesamientoHistoricoEventoListener(
            ProcesamientoHistoricoWorker worker) {
        this.worker = worker;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alSolicitarProcesamiento(
            ProcesamientoHistoricoSolicitadoEvent evento) {

        try {
            worker.ejecutar(evento.idProcesamiento());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo encolar el procesamiento histórico {}.",
                    evento.idProcesamiento(), ex);
        }
    }
}
