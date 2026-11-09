package org.mgroko.backend.presencia.evento;

import java.time.LocalDateTime;

/**
 * Evento interno que se publica al registrar un heartbeat. Se procesa después
 * del commit para no emitir presencia si la transacción se revierte.
 */
public record PresenciaActividadRegistrada(
        Long idPerfil,
        LocalDateTime ahora,
        LocalDateTime limite
) {
}
