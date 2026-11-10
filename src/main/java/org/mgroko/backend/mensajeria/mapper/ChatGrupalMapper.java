package org.mgroko.backend.mensajeria.mapper;

import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalStreamResponse;
import org.mgroko.backend.mensajeria.dto.ParticipanteChatGrupalResponse;
import org.mgroko.backend.modelo.MensajeProyecto;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;

public final class ChatGrupalMapper {

    private ChatGrupalMapper() {
    }

    public static ParticipanteChatGrupalResponse toParticipante(MiembroProyecto miembro) {
        RolProyectoUsuario rol = RolProyectoUsuario.desde(miembro.getRolProyecto());

        return new ParticipanteChatGrupalResponse(
                MensajeriaMapper.toPerfilMensaje(miembro.getPerfil()),
                rol != null ? rol.name() : null);
    }

    public static MensajeGrupalResponse toMensajeResponse(
            MensajeProyecto mensaje,
            Long idPerfilActivo) {

        Long idPerfilEmisor = mensaje.getPerfilEmisor().getIdPerfil();

        return new MensajeGrupalResponse(
                mensaje.getIdMensajeProyecto(),
                mensaje.getProyecto().getIdProyecto(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                idPerfilEmisor,
                MensajeriaMapper.toPerfilMensaje(mensaje.getPerfilEmisor()),
                idPerfilEmisor.equals(idPerfilActivo));
    }

    public static MensajeGrupalStreamResponse toStreamResponse(MensajeProyecto mensaje) {
        return new MensajeGrupalStreamResponse(
                mensaje.getIdMensajeProyecto(),
                mensaje.getProyecto().getIdProyecto(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                mensaje.getPerfilEmisor().getIdPerfil(),
                MensajeriaMapper.toPerfilMensaje(mensaje.getPerfilEmisor()));
    }
}
