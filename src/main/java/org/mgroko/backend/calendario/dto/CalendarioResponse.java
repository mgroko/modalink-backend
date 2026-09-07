package org.mgroko.backend.calendario.dto;

import java.util.List;

/**
 * Representación completa del calendario del usuario (estilo Google
 * Calendar): configuración de jornada, bloqueos manuales y bloqueos
 * calculados por actividades de proyectos activos.
 */
public record CalendarioResponse(
        ConfigJornadaResponse jornada,
        List<BloqueoResponse> bloqueosManuales,
        List<BloqueoActividadResponse> actividades
) {
}