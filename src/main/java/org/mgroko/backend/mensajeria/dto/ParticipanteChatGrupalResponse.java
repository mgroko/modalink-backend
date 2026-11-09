package org.mgroko.backend.mensajeria.dto;

/**
 * Participante activo del chat grupal de un proyecto.
 */
public record ParticipanteChatGrupalResponse(
        PerfilMensajeResponse perfil,
        String rol
) {
}
