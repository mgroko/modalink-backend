package org.mgroko.backend.invitaciones.dto;

import java.util.List;

/**
 * Resultado de una invitación múltiple. Permite éxito parcial: las
 * invitaciones creadas y los errores por perfil se informan por separado.
 */
public record InvitacionBatchResponse(
        List<InvitacionResponse> creadas,
        List<InvitacionBatchError> errores
) {
    public InvitacionBatchResponse {
        creadas = creadas == null ? List.of() : List.copyOf(creadas);
        errores = errores == null ? List.of() : List.copyOf(errores);
    }
}
