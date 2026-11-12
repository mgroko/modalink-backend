package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.publicaciones.evento.ColaboracionAutomaticaSolicitadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara la detección automática después de que la transacción de subida
 * confirma.
 *
 * <p>El listener corre en el hilo que confirma la transacción y solo encola el
 * trabajo; la detección se ejecuta en el executor dedicado.</p>
 */
@Component
public class ColaboracionAutomaticaEventoListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ColaboracionAutomaticaEventoListener.class);

    private final DetectarColaboracionAutomaticaAsyncService asyncService;

    public ColaboracionAutomaticaEventoListener(
            DetectarColaboracionAutomaticaAsyncService asyncService) {
        this.asyncService = asyncService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alSolicitarDeteccion(ColaboracionAutomaticaSolicitadaEvent evento) {
        try {
            asyncService.procesar(evento.idPublicacion(), evento.idsImagenes());
        } catch (RuntimeException ex) {
            // Si el executor rechaza la tarea, la subida ya está confirmada.
            LOG.warn("No se pudo encolar la detección automática para la publicación {}.",
                    evento.idPublicacion(), ex);
        }
    }
}
