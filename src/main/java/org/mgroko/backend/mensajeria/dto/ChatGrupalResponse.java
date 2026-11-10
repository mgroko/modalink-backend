package org.mgroko.backend.mensajeria.dto;

import java.util.List;

/**
 * Datos de apertura del chat grupal de un proyecto.
 */
public record ChatGrupalResponse(
        Long idProyecto,
        String nombreProyecto,
        String estadoProyecto,
        boolean puedeEnviar,
        List<ParticipanteChatGrupalResponse> participantes
) {
}
