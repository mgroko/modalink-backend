package org.mgroko.backend.notificaciones.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.proyectos.evento.ProyectoFinalizadoEvent;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * UC-37: solicita a los participantes activos que califiquen al equipo una vez
 * finalizado el proyecto.
 *
 * <p>Se ejecuta después del commit: un fallo de notificación no revierte la
 * finalización ni las calificaciones del director.</p>
 */
@Component
public class ProyectoFinalizadoNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProyectoFinalizadoNotificacionListener.class);

    private static final String ENTIDAD_PROYECTO = "PROYECTO";

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;

    public ProyectoFinalizadoNotificacionListener(
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alFinalizarProyecto(ProyectoFinalizadoEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las solicitudes de calificación del proyecto {}.",
                    evento.idProyecto(), ex);
        }
    }

    private void registrarNotificaciones(ProyectoFinalizadoEvent evento) {
        List<MiembroProyecto> miembros = miembroProyectoRepository
                .buscarMiembrosActivosDeProyecto(evento.idProyecto());

        Set<Long> perfilesNotificados = new HashSet<>();
        List<Notificacion> notificaciones = new ArrayList<>();
        String mensaje = "El proyecto '" + evento.nombreProyecto()
                + "' finalizó. Calificá a los integrantes del equipo.";

        for (MiembroProyecto miembro : miembros) {
            Perfil perfil = miembro.getPerfil();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (perfil == null || usuario == null) {
                continue;
            }
            if (evento.idPerfilActor() != null
                    && evento.idPerfilActor().equals(perfil.getIdPerfil())) {
                continue;
            }
            if (!perfilesNotificados.add(perfil.getIdPerfil())) {
                continue;
            }

            notificaciones.add(notificacionService.crear(
                    usuario,
                    perfil,
                    NotificacionService.TIPO_CALIFICACION_SOLICITADA,
                    mensaje,
                    ENTIDAD_PROYECTO,
                    evento.idProyecto(),
                    evento.idProyecto()));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
