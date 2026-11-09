package org.mgroko.backend.mensajeria.mapper;

import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.dto.MensajeStreamResponse;
import org.mgroko.backend.mensajeria.dto.PerfilMensajeResponse;
import org.mgroko.backend.mensajeria.dto.UltimoMensajeRow;
import org.mgroko.backend.modelo.Conversacion;
import org.mgroko.backend.modelo.Mensaje;
import org.mgroko.backend.modelo.Perfil;

public final class MensajeriaMapper {

    private static final int MAX_PREVIEW = 120;

    private MensajeriaMapper() {
    }

    public static PerfilMensajeResponse toPerfilMensaje(Perfil perfil) {
        if (perfil == null) {
            return null;
        }

        var usuario = perfil.getUsuario();
        var profesion = perfil.getProfesion();
        var imagen = perfil.getImagen();

        return new PerfilMensajeResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getApellido() : null,
                profesion != null ? profesion.getIdProfesion() : null,
                profesion != null ? profesion.getNombre() : null,
                imagen != null ? imagen.getUrl() : null);
    }

    public static ConversacionResponse toConversacionResponse(
            Conversacion conversacion,
            Long idPerfilActivo,
            UltimoMensajeRow ultimo,
            boolean tieneNoLeidos,
            boolean puedeEnviar) {

        Perfil contraparte = contraparte(conversacion, idPerfilActivo);

        return new ConversacionResponse(
                conversacion.getIdConversacion(),
                toPerfilMensaje(contraparte),
                conversacion.getFechaCreacion(),
                conversacion.getFechaUltimoMensaje(),
                ultimo != null ? truncar(ultimo.contenido()) : null,
                tieneNoLeidos,
                puedeEnviar);
    }

    public static MensajeResponse toMensajeResponse(Mensaje mensaje, Long idPerfilActivo) {
        return new MensajeResponse(
                mensaje.getIdMensaje(),
                mensaje.getConversacion().getIdConversacion(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                mensaje.getPerfilEmisor().getIdPerfil(),
                toPerfilMensaje(mensaje.getPerfilEmisor()),
                mensaje.getPerfilEmisor().getIdPerfil().equals(idPerfilActivo),
                Boolean.TRUE.equals(mensaje.getLeido()),
                mensaje.getFechaLectura());
    }

    public static MensajeStreamResponse toMensajeStreamResponse(Mensaje mensaje) {
        return new MensajeStreamResponse(
                mensaje.getIdMensaje(),
                mensaje.getConversacion().getIdConversacion(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                mensaje.getPerfilEmisor().getIdPerfil(),
                toPerfilMensaje(mensaje.getPerfilEmisor()),
                Boolean.TRUE.equals(mensaje.getLeido()),
                mensaje.getFechaLectura());
    }

    public static Perfil contraparte(Conversacion conversacion, Long idPerfilActivo) {
        return conversacion.getPerfil1().getIdPerfil().equals(idPerfilActivo)
                ? conversacion.getPerfil2()
                : conversacion.getPerfil1();
    }

    private static String truncar(String contenido) {
        if (contenido == null || contenido.length() <= MAX_PREVIEW) {
            return contenido;
        }
        return contenido.substring(0, MAX_PREVIEW) + "…";
    }
}
