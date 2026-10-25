package org.mgroko.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del sistema inyectable. Los servicios de negocio que necesitan la
 * fecha actual deben depender de este {@link Clock} en lugar de llamar a
 * {@code LocalDate.now()} o {@code LocalDateTime.now()} directamente, para
 * poder fijar el tiempo en los tests.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
