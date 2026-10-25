package org.mgroko.backend.notificaciones.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.evento.ActividadModificadaEvent;
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
public class ActividadModificadaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ActividadModificadaNotificacionListener.class);

    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public ActividadModificadaNotificacionListener(
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alModificarActividad(ActividadModificadaEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la actividad {}.",
                    evento.idActividad(), ex);
        }
    }

    private void registrarNotificaciones(ActividadModificadaEvent evento) {
        if (evento.idsPerfilesDestinatarios() == null
                || evento.idsPerfilesDestinatarios().isEmpty()) {
            return;
        }

        Set<Long> idsDestinatarios = new LinkedHashSet<>(evento.idsPerfilesDestinatarios());
        Map<Long, Perfil> perfilesPorId = new LinkedHashMap<>();
        for (Perfil perfil : perfilRepository.findAllById(idsDestinatarios)) {
            perfilesPorId.put(perfil.getIdPerfil(), perfil);
        }

        if (perfilesPorId.isEmpty()) {
            return;
        }

        String mensaje = "La actividad \"" + evento.nombreActividad()
                + "\" del proyecto \"" + evento.nombreProyecto() + "\" fue modificada.";

        List<Notificacion> notificaciones = new ArrayList<>();
        for (Perfil perfil : perfilesPorId.values()) {
            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_ACTIVIDAD_MODIFICADA,
                    mensaje,
                    "ACTIVIDAD",
                    evento.idActividad()));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
