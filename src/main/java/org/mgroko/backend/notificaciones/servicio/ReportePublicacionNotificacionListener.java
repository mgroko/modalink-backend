package org.mgroko.backend.notificaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.reportes.evento.ReportePublicacionCreadoEvent;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * UC-69: alerta in-app a los administradores cuando se registra un reporte de
 * publicación.
 *
 * <p>Se ejecuta después del commit para no notificar reportes que terminen
 * revertidos. El reporte ya está persistido; si no hay administradores activos
 * o falla la notificación, se loguea y el reporte se conserva.</p>
 */
@Component
public class ReportePublicacionNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReportePublicacionNotificacionListener.class);

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ReportePublicacionNotificacionListener(
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearReporte(ReportePublicacionCreadoEvent evento) {
        try {
            List<Usuario> administradores = usuarioRepository.findAdministradoresActivos();

            if (administradores.isEmpty()) {
                LOG.warn(
                        "Reporte de publicación {} guardado sin notificación: no hay administradores activos.",
                        evento.idReporte());
                return;
            }

            String mensaje = construirMensaje(evento);
            List<Notificacion> notificaciones = administradores.stream()
                    .map(administrador -> notificacionService.crear(
                            administrador,
                            null,
                            NotificacionService.TIPO_PUBLICACION_REPORTADA,
                            mensaje,
                            NotificacionDestinoResolver.ENTIDAD_REPORTE_PUBLICACION,
                            evento.idReporte(),
                            null))
                    .toList();

            notificacionService.registrarTodas(notificaciones);
        } catch (RuntimeException ex) {
            LOG.warn(
                    "No se pudieron registrar las notificaciones del reporte de publicación {}.",
                    evento.idReporte(),
                    ex);
        }
    }

    private String construirMensaje(ReportePublicacionCreadoEvent evento) {
        String descripcion = evento.descripcion() != null
                ? evento.descripcion()
                : "Sin descripción";

        return "Nuevo reporte de " + evento.nombreUsuarioAutor()
                + " (" + evento.nombrePerfilAutor() + ") contra la publicación '"
                + evento.tituloPublicacion() + "' del perfil '"
                + evento.nombrePerfilPublicacion() + "'. Motivo: "
                + evento.motivo().getDescripcion()
                + ". Descripción: " + descripcion + ".";
    }
}
