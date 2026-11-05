package org.mgroko.backend.invitaciones.dto;

/**
 * Error de un perfil dentro de una invitación múltiple.
 */
public record InvitacionBatchError(
        Long idPerfilDestinatario,
        String mensaje,
        String codigo
) {
}
