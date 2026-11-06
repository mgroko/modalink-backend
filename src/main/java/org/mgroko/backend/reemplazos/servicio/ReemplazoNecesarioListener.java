package org.mgroko.backend.reemplazos.servicio;

import org.mgroko.backend.reemplazos.evento.ReemplazoNecesarioEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha la baja/eliminación después del commit.
 *
 * <p>Si la generación de reemplazos falla, la baja ya está persistida y no se
 * revierte. El error se registra para poder reintentar o diagnosticar.</p>
 */
@Component
public class ReemplazoNecesarioListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReemplazoNecesarioListener.class);

    private final GenerarReemplazosService generarReemplazosService;

    public ReemplazoNecesarioListener(GenerarReemplazosService generarReemplazosService) {
        this.generarReemplazosService = generarReemplazosService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alDetectarBaja(ReemplazoNecesarioEvent evento) {
        try {
            generarReemplazosService.generar(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo generar el reemplazo para el miembro {} del proyecto {}.",
                    evento.idMiembroBaja(), evento.idProyecto(), ex);
        }
    }
}
