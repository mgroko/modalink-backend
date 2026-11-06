package org.mgroko.backend.notificaciones.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.proyectos.evento.ProyectoConfirmadoEvent;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifica a todos los miembros activos que el proyecto pasó a Confirmado.
 *
 * <p>Se ejecuta después del commit: un fallo de notificación no revierte la
 * confirmación ni los horarios persistidos.</p>
 */
@Component
public class ProyectoConfirmadoNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProyectoConfirmadoNotificacionListener.class);

    private static final String ENTIDAD_PROYECTO = "PROYECTO";

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;

    public ProyectoConfirmadoNotificacionListener(
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alConfirmarProyecto(ProyectoConfirmadoEvent evento) {
        try {
            registrarNotificaciones(evento);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron registrar las notificaciones del proyecto confirmado {}.",
                    evento.idProyecto(), ex);
        }
    }

    private void registrarNotificaciones(ProyectoConfirmadoEvent evento) {
        List<MiembroProyecto> miembros = miembroProyectoRepository
                .buscarMiembrosActivosDeProyecto(evento.idProyecto());

        Set<Long> perfilesNotificados = new HashSet<>();
        List<Notificacion> notificaciones = new ArrayList<>();
        String mensaje = "El proyecto '" + evento.nombreProyecto() + "' fue confirmado.";

        for (MiembroProyecto miembro : miembros) {
            Perfil perfil = miembro.getPerfil();
            Usuario usuario = perfil != null ? perfil.getUsuario() : null;
            if (perfil == null || usuario == null) {
                continue;
            }
            if (!perfilesNotificados.add(perfil.getIdPerfil())) {
                continue;
            }

            notificaciones.add(notificacionService.crear(
                    usuario,
                    perfil,
                    NotificacionService.TIPO_PROYECTO_CONFIRMADO,
                    mensaje,
                    ENTIDAD_PROYECTO,
                    evento.idProyecto(),
                    evento.idProyecto()));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
