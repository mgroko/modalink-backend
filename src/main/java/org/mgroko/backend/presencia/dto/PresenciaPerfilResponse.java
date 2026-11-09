package org.mgroko.backend.presencia.dto;

/**
 * Estado de presencia de un perfil. Es el único dato que se expone: nunca la
 * última actividad.
 */
public record PresenciaPerfilResponse(
        Long idPerfil,
        boolean enLinea
) {
}
