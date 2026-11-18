package org.mgroko.backend.admin.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar, los jobs que quedaron en progreso se marcan como interrumpidos
 * para que el administrador pueda reanudarlos.
 */
@Component
public class ProcesamientoHistoricoStartupService {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProcesamientoHistoricoStartupService.class);

    private final ProcesamientoColaboracionHistoricaRepository repository;
    private final Clock reloj;

    public ProcesamientoHistoricoStartupService(
            ProcesamientoColaboracionHistoricaRepository repository,
            Clock reloj) {
        this.repository = repository;
        this.reloj = reloj;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void marcarInterrumpidos() {
        int actualizados = repository.marcarInterrumpidos(
                java.util.List.of(
                        EstadoProcesamientoHistorico.PENDIENTE,
                        EstadoProcesamientoHistorico.EN_PROGRESO),
                EstadoProcesamientoHistorico.INTERRUMPIDO,
                LocalDateTime.now(reloj));

        if (actualizados > 0) {
            LOG.warn("Se marcaron {} procesamientos históricos como interrumpidos.",
                    actualizados);
        }
    }
}
