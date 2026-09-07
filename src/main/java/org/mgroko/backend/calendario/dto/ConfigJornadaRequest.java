package org.mgroko.backend.calendario.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Configuración completa de la jornada laboral del usuario. Reemplaza de
 * forma total la jornada existente: el conjunto de días presentes define
 * los días laborables (día ausente = no laborable) y el margen por
 * actividad se actualiza junto con ellos.
 */
public record ConfigJornadaRequest(
        @NotNull Integer margenActividadMinutos,
        @NotEmpty List<@Valid JornadaDiaRequest> dias
) {
}