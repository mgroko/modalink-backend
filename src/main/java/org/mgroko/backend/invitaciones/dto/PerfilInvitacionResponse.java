package org.mgroko.backend.invitaciones.dto;

/**
 * Datos mínimos del perfil remitente o destinatario de una invitación.
 */
public record PerfilInvitacionResponse(
        Long idPerfil,
        String nombreArtistico,
        String nombre,
        String apellido,
        Long idProfesion,
        String nombreProfesion
) {
}
