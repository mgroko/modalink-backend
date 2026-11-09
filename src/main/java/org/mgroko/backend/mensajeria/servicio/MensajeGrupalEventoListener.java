package org.mgroko.backend.mensajeria.servicio;

import org.mgroko.backend.mensajeria.dto.MensajeGrupalEvento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica los eventos SSE del chat grupal después del commit.
 */
@Component
public class MensajeGrupalEventoListener {

    private final MensajeEventBus eventBus;

    public MensajeGrupalEventoListener(MensajeEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alEnviarMensajeGrupal(MensajeGrupalEvento evento) {
        eventBus.publicarMensajeGrupal(evento);
    }
}
