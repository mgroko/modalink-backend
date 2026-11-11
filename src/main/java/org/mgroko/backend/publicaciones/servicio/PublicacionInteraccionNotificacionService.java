package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: notifica las interacciones al perfil dueño y a los colaboradores
 * activos de la publicación, excluyendo al perfil que interactúa.
 */
@Service
@Transactional
public class PublicacionInteraccionNotificacionService {

    private final NotificacionService notificacionService;

    public PublicacionInteraccionNotificacionService(
            NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    public void notificarMeGusta(Publicacion publicacion, Perfil perfilActor) {
        String mensaje = "El perfil '" + perfilActor.getNombreArtistico()
                + "' le dio Me gusta a la publicación '"
                + publicacion.getTitulo() + "'.";

        notificar(
                publicacion,
                perfilActor,
                NotificacionService.TIPO_PUBLICACION_MEGUSTA,
                mensaje);
    }

    public void notificarComentario(Publicacion publicacion, Perfil perfilActor) {
        String mensaje = "El perfil '" + perfilActor.getNombreArtistico()
                + "' comentó la publicación '"
                + publicacion.getTitulo() + "'.";

        notificar(
                publicacion,
                perfilActor,
                NotificacionService.TIPO_PUBLICACION_COMENTARIO,
                mensaje);
    }

    private void notificar(
            Publicacion publicacion,
            Perfil perfilActor,
            String tipo,
            String mensaje) {

        List<Perfil> destinatarios = resolverDestinatarios(publicacion, perfilActor);
        if (destinatarios.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new ArrayList<>(destinatarios.size());
        for (Perfil destinatario : destinatarios) {
            notificaciones.add(notificacionService.crear(
                    destinatario.getUsuario(),
                    destinatario,
                    tipo,
                    mensaje,
                    NotificacionService.ENTIDAD_PUBLICACION,
                    publicacion.getIdPublicacion()));
        }

        notificacionService.registrarTodas(notificaciones);
    }

    private List<Perfil> resolverDestinatarios(
            Publicacion publicacion,
            Perfil perfilActor) {

        Map<Long, Perfil> destinatarios = new LinkedHashMap<>();

        agregarSiCorresponde(destinatarios, publicacion.getPerfil(), perfilActor);

        if (publicacion.getColaboradores() != null) {
            for (Perfil colaborador : publicacion.getColaboradores()) {
                agregarSiCorresponde(destinatarios, colaborador, perfilActor);
            }
        }

        return new ArrayList<>(destinatarios.values());
    }

    private void agregarSiCorresponde(
            Map<Long, Perfil> destinatarios,
            Perfil perfil,
            Perfil perfilActor) {

        if (perfil == null || perfil.getIdPerfil() == null) {
            return;
        }

        if (perfil.getIdPerfil().equals(perfilActor.getIdPerfil())) {
            return;
        }

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            return;
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            return;
        }

        destinatarios.putIfAbsent(perfil.getIdPerfil(), perfil);
    }
}
