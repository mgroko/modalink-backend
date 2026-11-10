package org.mgroko.backend.presencia.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PresenciaRegistryTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 9, 12, 0);
    private static final LocalDateTime LIMITE = AHORA.minusSeconds(120);

    private final PresenciaRegistry registry = new PresenciaRegistry();

    @Test
    void primerHeartbeat_emiteTransicionOnline() {
        assertThat(registry.registrar(1L, AHORA, LIMITE)).isTrue();
        assertThat(registry.estaRegistrado(1L)).isTrue();
    }

    @Test
    void heartbeatDentroDelUmbral_noEmiteNuevaTransicion() {
        registry.registrar(1L, AHORA, LIMITE);

        assertThat(registry.registrar(1L, AHORA.plusSeconds(30), LIMITE)).isFalse();
    }

    @Test
    void heartbeatDespuesDelUmbral_emiteTransicionOnline() {
        registry.registrar(1L, AHORA, LIMITE);

        LocalDateTime despues = AHORA.plusSeconds(121);
        assertThat(registry.registrar(1L, despues, despues.minusSeconds(120))).isTrue();
    }

    @Test
    void expirar_eliminaSoloLosVencidos() {
        registry.registrar(1L, AHORA.minusSeconds(121), LIMITE.minusSeconds(1));
        registry.registrar(2L, AHORA, LIMITE);

        List<Long> expirados = registry.expirar(LIMITE);

        assertThat(expirados).containsExactly(1L);
        assertThat(registry.estaRegistrado(1L)).isFalse();
        assertThat(registry.estaRegistrado(2L)).isTrue();
    }

    @Test
    void expirar_noEliminaUnHeartbeatConcurrente() {
        registry.registrar(1L, AHORA.minusSeconds(121), LIMITE.minusSeconds(1));

        // Simula un heartbeat que llegó justo antes de la expiración.
        registry.registrar(1L, AHORA, LIMITE);

        assertThat(registry.expirar(LIMITE)).isEmpty();
        assertThat(registry.estaRegistrado(1L)).isTrue();
    }
}
