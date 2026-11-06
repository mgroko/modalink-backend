package org.mgroko.backend.calendario.dto;

/**
 * Perfil del usuario dueño del calendario que está asignado a una actividad.
 * Permite distinguir de qué perfil proviene cada bloqueo calculado cuando el
 * usuario tiene más de un perfil.
 */
public record PerfilCalendarioResponse(
        Long idPerfil,
        String nombreArtistico
) {
}
