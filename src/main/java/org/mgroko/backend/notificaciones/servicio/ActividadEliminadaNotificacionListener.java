package org.mgroko.backend.notificaciones.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.InvitacionAfectada;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.PostulacionAfectada;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActividadEliminadaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ActividadEliminadaNotificacionListener.class);

    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public ActividadEliminadaNotificacionListener(
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alEliminarActividad(ActividadEliminadaEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la actividad eliminada {}.",
                    evento.idActividad(), ex);
        }
    }

    private void registrarNotificaciones(ActividadEliminadaEvent evento) {
        Set<Long> idsDestinatarios = new LinkedHashSet<>();
        idsDestinatarios.addAll(evento.idsPerfilesAsignados());
        for (PostulacionAfectada postulacion : evento.postulaciones()) {
            idsDestinatarios.add(postulacion.idPerfil());
        }
        for (InvitacionAfectada invitacion : evento.invitaciones()) {
            idsDestinatarios.add(invitacion.idPerfil());
        }

        if (idsDestinatarios.isEmpty()) {
            return;
        }

        Map<Long, Perfil> perfilesPorId = new LinkedHashMap<>();
        for (Perfil perfil : perfilRepository.findAllById(idsDestinatarios)) {
            perfilesPorId.put(perfil.getIdPerfil(), perfil);
        }

        if (perfilesPorId.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new ArrayList<>();

        String mensajeActividad = "La actividad \"" + evento.nombreActividad()
                + "\" del proyecto \"" + evento.nombreProyecto()
                + "\" fue eliminada. Motivo: " + evento.motivo();

        for (Long idPerfil : evento.idsPerfilesAsignados()) {
            Perfil perfil = perfilesPorId.get(idPerfil);
            if (perfil == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                    mensajeActividad,
                    "ACTIVIDAD",
                    evento.idActividad(),
                    evento.idProyecto()));
        }

        String mensajePostulacion = "Tu postulación a la actividad \""
                + evento.nombreActividad() + "\" del proyecto \""
                + evento.nombreProyecto()
                + "\" quedó sin efecto porque la actividad fue eliminada. Motivo: "
                + evento.motivo();

        for (PostulacionAfectada postulacion : evento.postulaciones()) {
            Perfil perfil = perfilesPorId.get(postulacion.idPerfil());
            if (perfil == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_POSTULACION_RECHAZADA,
                    mensajePostulacion,
                    "POSTULACION_ACTIVIDAD",
                    postulacion.idPostulacion(),
                    evento.idProyecto()));
        }

        String mensajeInvitacion = "Tu invitación a la actividad \""
                + evento.nombreActividad() + "\" del proyecto \""
                + evento.nombreProyecto()
                + "\" quedó sin efecto porque la actividad fue eliminada. Motivo: "
                + evento.motivo();

        for (InvitacionAfectada invitacion : evento.invitaciones()) {
            Perfil perfil = perfilesPorId.get(invitacion.idPerfil());
            if (perfil == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_INVITACION_RECHAZADA,
                    mensajeInvitacion,
                    "INVITACION_ACTIVIDAD",
                    invitacion.idInvitacion(),
                    evento.idProyecto()));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
