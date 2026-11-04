package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.proyectos.evento.DisponibilidadModificadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reprograma los proyectos afectados cuando cambia la disponibilidad de un
 * usuario.
 *
 * <p>Se ejecuta después del commit para no revertir la operación original si
 * la reprogramación falla. Cada proyecto se reprograma en su propia
 * transacción dentro de {@link JornadaReprogramacionService}.</p>
 */
@Component
public class DisponibilidadModificadaListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(DisponibilidadModificadaListener.class);

    private final JornadaReprogramacionService reprogramacionService;

    public DisponibilidadModificadaListener(
            JornadaReprogramacionService reprogramacionService) {
        this.reprogramacionService = reprogramacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void alModificarDisponibilidad(DisponibilidadModificadaEvent evento) {
        if (evento == null || evento.idUsuario() == null) {
            return;
        }
        try {
            reprogramacionService.reprogramarAfectados(
                    evento.idUsuario(),
                    evento.motivo(),
                    evento.idProyectoExcluido());
        } catch (RuntimeException ex) {
            LOG.warn(
                    "No se pudo reprogramar la disponibilidad modificada del usuario {}.",
                    evento.idUsuario(),
                    ex);
        }
    }
}
