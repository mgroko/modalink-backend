package org.mgroko.backend.presencia.dto;

/**
 * Evento SSE de cambio de presencia para un perfil.
 */
public record PresenciaEvento(
        Long idPerfil,
        boolean enLinea
) {
}
