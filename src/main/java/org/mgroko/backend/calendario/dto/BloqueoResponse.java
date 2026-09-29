package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

/**
 * Bloqueo manual de la agenda (período "No disponible" definido por el
 * usuario). El motivo es obligatorio (NOT NULL en BD) y solo aplica a los
 * bloqueos manuales; en la vista pública se oculta (null).
 */
public record BloqueoResponse(
        Long idBloqueo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivo
) {
}