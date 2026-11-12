package org.mgroko.backend.notificaciones.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpia tokens de auto-login vencidos o usados.
 */
@Component
@ConditionalOnProperty(
        name = "app.notificaciones.email.habilitado",
        havingValue = "true")
public class TokenAccesoEmailCleanupScheduler {

    private static final Logger LOG =
            LoggerFactory.getLogger(TokenAccesoEmailCleanupScheduler.class);

    private final TokenAccesoEmailService tokenService;

    public TokenAccesoEmailCleanupScheduler(TokenAccesoEmailService tokenService) {
        this.tokenService = tokenService;
    }

    @Scheduled(cron = "0 30 4 * * *")
    public void limpiar() {
        int eliminados = tokenService.limpiarVencidos();
        if (eliminados > 0) {
            LOG.info("Se eliminaron {} tokens de acceso por email vencidos o usados.", eliminados);
        }
    }
}
