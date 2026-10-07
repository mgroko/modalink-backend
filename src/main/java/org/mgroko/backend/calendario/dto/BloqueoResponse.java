package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;

/**
 * Bloqueo manual de la agenda (período "No disponible" definido por el
 * usuario). El motivo es obligatorio (NOT NULL en BD), solo aplica a los
 * bloqueos manuales y se devuelve siempre informado, también en la vista
 * pública (GET /calendario/perfil/{id}).
 */
public record BloqueoResponse(
        Long idBloqueo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivo
) {
}