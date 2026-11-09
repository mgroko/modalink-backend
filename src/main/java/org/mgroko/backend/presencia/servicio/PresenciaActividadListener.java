package org.mgroko.backend.presencia.servicio;

import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.mgroko.backend.presencia.evento.PresenciaActividadRegistrada;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Actualiza el registro en memoria y emite el evento online después del
 * commit. Se actualiza en cada heartbeat, incluso cuando la escritura en base
 * se omite por throttle.
 */
@Component
public class PresenciaActividadListener {

    private final PresenciaRegistry registry;
    private final PresenciaEventBus eventBus;

    public PresenciaActividadListener(
            PresenciaRegistry registry,
            PresenciaEventBus eventBus) {
        this.registry = registry;
        this.eventBus = eventBus;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alRegistrarActividad(PresenciaActividadRegistrada evento) {
        boolean transicion = registry.registrar(
                evento.idPerfil(), evento.ahora(), evento.limite());

        if (transicion) {
            eventBus.publicar(new PresenciaEvento(evento.idPerfil(), true));
        }
    }
}
