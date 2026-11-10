package org.mgroko.backend.mensajeria.servicio;

import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.EscribiendoEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica eventos SSE después del commit para no avisar mensajes que luego se
 * revierten.
 */
@Component
public class MensajeEventoListener {

    private final MensajeEventBus eventBus;

    public MensajeEventoListener(MensajeEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alEnviarMensaje(MensajeEvento evento) {
        eventBus.publicarMensaje(evento);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alLeerConversacion(ConversacionLeidaEvento evento) {
        eventBus.publicarLeido(evento);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alEscribir(EscribiendoEvento evento) {
        eventBus.publicarEscribiendo(evento);
    }
}
