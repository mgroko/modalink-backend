package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

/**
 * Bloqueo manual de la agenda (período "No disponible" definido por el
 * usuario). El motivo es opcional y solo aplica a los bloqueos manuales.
 */
public record BloqueoResponse(
        Long idBloqueo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivo
) {
}