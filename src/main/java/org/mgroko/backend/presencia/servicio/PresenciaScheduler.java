package org.mgroko.backend.presencia.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.presencia.dto.PresenciaEvento;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expira perfiles sin heartbeat reciente y mantiene vivas las conexiones SSE.
 */
@Component
@ConditionalOnProperty(
        name = "app.presencia.scheduling-habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class PresenciaScheduler {

    private final PresenciaRegistry registry;
    private final PresenciaEventBus eventBus;
    private final PresenciaService presenciaService;

    public PresenciaScheduler(
            PresenciaRegistry registry,
            PresenciaEventBus eventBus,
            PresenciaService presenciaService) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.presenciaService = presenciaService;
    }

    @Scheduled(fixedDelayString = "${app.presencia.intervalo-expiracion-ms:5000}")
    public void expirar() {
        LocalDateTime ahora = presenciaService.ahora();
        LocalDateTime limite = presenciaService.limite(ahora);

        List<Long> expirados = registry.expirar(limite);
        for (Long idPerfil : expirados) {
            eventBus.publicar(new PresenciaEvento(idPerfil, false));
        }
    }

    @Scheduled(fixedDelayString = "${app.presencia.intervalo-ping-ms:20000}")
    public void ping() {
        eventBus.ping();
    }
}
