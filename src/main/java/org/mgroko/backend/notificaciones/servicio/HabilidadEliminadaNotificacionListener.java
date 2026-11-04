package org.mgroko.backend.notificaciones.servicio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.evento.HabilidadEliminadaEvent;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HabilidadEliminadaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(HabilidadEliminadaNotificacionListener.class);

    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public HabilidadEliminadaNotificacionListener(
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alEliminarHabilidad(HabilidadEliminadaEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la habilidad eliminada {}.",
                    evento.idHabilidad(), ex);
        }
    }

    private void registrarNotificaciones(HabilidadEliminadaEvent evento) {
        if (evento.idsPerfilesDestinatarios() == null
                || evento.idsPerfilesDestinatarios().isEmpty()) {
            return;
        }

        Set<Long> idsDestinatarios = new LinkedHashSet<>(evento.idsPerfilesDestinatarios());
        List<Perfil> perfiles = perfilRepository.findAllById(idsDestinatarios);
        if (perfiles.isEmpty()) {
            return;
        }

        String mensaje = "La habilidad \"" + evento.nombreHabilidad()
                + "\" fue dada de baja del catálogo.";

        List<Notificacion> notificaciones = perfiles.stream()
                .map(perfil -> notificacionService.crear(
                        perfil.getUsuario(),
                        perfil,
                        NotificacionService.TIPO_HABILIDAD_ELIMINADA,
                        mensaje,
                        "HABILIDAD",
                        evento.idHabilidad()))
                .toList();

        notificacionService.registrarTodas(notificaciones);
    }
}
