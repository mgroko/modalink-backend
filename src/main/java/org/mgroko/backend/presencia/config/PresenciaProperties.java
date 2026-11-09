package org.mgroko.backend.presencia.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de presencia por perfil.
 *
 * <p>{@code umbralEnLinea} debe ser mayor que el intervalo de heartbeat para
 * tolerar un heartbeat perdido. Los valores por defecto son 120 s y 30 s.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.presencia")
public class PresenciaProperties {

    private Duration umbralEnLinea = Duration.ofSeconds(120);
    private Duration timeoutStream = Duration.ofMinutes(30);
    private long intervaloExpiracionMs = 5000;
    private long intervaloPingMs = 20000;
    private int maxIds = 200;

    public Duration getUmbralEnLinea() {
        return umbralEnLinea;
    }

    public void setUmbralEnLinea(Duration umbralEnLinea) {
        this.umbralEnLinea = umbralEnLinea;
    }

    public Duration getTimeoutStream() {
        return timeoutStream;
    }

    public void setTimeoutStream(Duration timeoutStream) {
        this.timeoutStream = timeoutStream;
    }

    public long getIntervaloExpiracionMs() {
        return intervaloExpiracionMs;
    }

    public void setIntervaloExpiracionMs(long intervaloExpiracionMs) {
        this.intervaloExpiracionMs = intervaloExpiracionMs;
    }

    public long getIntervaloPingMs() {
        return intervaloPingMs;
    }

    public void setIntervaloPingMs(long intervaloPingMs) {
        this.intervaloPingMs = intervaloPingMs;
    }

    public int getMaxIds() {
        return maxIds;
    }

    public void setMaxIds(int maxIds) {
        this.maxIds = maxIds;
    }
}
