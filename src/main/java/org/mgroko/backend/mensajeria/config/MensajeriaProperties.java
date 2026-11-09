package org.mgroko.backend.mensajeria.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mensajeria")
public class MensajeriaProperties {

    private Duration timeoutStream = Duration.ofMinutes(30);
    private long intervaloPingMs = 20000;

    public Duration getTimeoutStream() {
        return timeoutStream;
    }

    public void setTimeoutStream(Duration timeoutStream) {
        this.timeoutStream = timeoutStream;
    }

    public long getIntervaloPingMs() {
        return intervaloPingMs;
    }

    public void setIntervaloPingMs(long intervaloPingMs) {
        this.intervaloPingMs = intervaloPingMs;
    }
}
