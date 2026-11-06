package org.mgroko.backend.notificaciones.servicio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.evento.HabilidadModificadaEvent;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HabilidadModificadaNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(HabilidadModificadaNotificacionListener.class);

    private final PerfilRepository perfilRepository;
    private final NotificacionService notificacionService;

    public HabilidadModificadaNotificacionListener(
            PerfilRepository perfilRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alModificarHabilidad(HabilidadModificadaEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones de la habilidad {}.",
                    evento.idHabilidad(), ex);
        }
    }

    private void registrarNotificaciones(HabilidadModificadaEvent evento) {
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
                + "\" del catálogo fue modificada.";

        List<Notificacion> notificaciones = perfiles.stream()
                .map(perfil -> notificacionService.crear(
                        perfil.getUsuario(),
                        perfil,
                        NotificacionService.TIPO_HABILIDAD_MODIFICADA,
                        mensaje,
                        "HABILIDAD",
                        evento.idHabilidad()))
                .toList();

        notificacionService.registrarTodas(notificaciones);
    }
}
