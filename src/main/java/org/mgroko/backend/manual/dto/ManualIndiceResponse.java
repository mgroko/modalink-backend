package org.mgroko.backend.manual.dto;

import java.util.List;

/**
 * Índice visible del manual para el rol autenticado.
 */
public record ManualIndiceResponse(
        String version,
        List<ManualCapituloResumen> capitulos) {
}
