package org.mgroko.backend.mensajeria.servicio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantiene vivas las conexiones SSE de mensajería.
 */
@Component
@ConditionalOnProperty(
        name = "app.mensajeria.scheduling-habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class MensajeriaScheduler {

    private final MensajeEventBus eventBus;

    public MensajeriaScheduler(MensajeEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Scheduled(fixedDelayString = "${app.mensajeria.intervalo-ping-ms:20000}")
    public void ping() {
        eventBus.ping();
    }
}
