package org.mgroko.backend.notificaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.reportes.evento.ReportePerfilCreadoEvent;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * UC-15: alerta in-app a los administradores cuando se registra un reporte de
 * perfil.
 *
 * <p>Se ejecuta después del commit para no notificar reportes que terminen
 * revertidos. El reporte ya está persistido; si no hay administradores activos
 * o falla la notificación, se loguea y el reporte se conserva.</p>
 */
@Component
public class ReportePerfilNotificacionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReportePerfilNotificacionListener.class);

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ReportePerfilNotificacionListener(
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearReporte(ReportePerfilCreadoEvent evento) {
        try {
            List<Usuario> administradores = usuarioRepository.findAdministradoresActivos();

            if (administradores.isEmpty()) {
                LOG.warn(
                        "Reporte de perfil {} guardado sin notificación: no hay administradores activos.",
                        evento.idReporte());
                return;
            }

            String mensaje = construirMensaje(evento);
            List<Notificacion> notificaciones = administradores.stream()
                    .map(administrador -> notificacionService.crear(
                            administrador,
                            null,
                            NotificacionService.TIPO_PERFIL_REPORTADO,
                            mensaje,
                            NotificacionDestinoResolver.ENTIDAD_REPORTE_PERFIL,
                            evento.idReporte(),
                            null))
                    .toList();

            notificacionService.registrarTodas(notificaciones);
        } catch (RuntimeException ex) {
            LOG.warn(
                    "No se pudieron registrar las notificaciones del reporte de perfil {}.",
                    evento.idReporte(),
                    ex);
        }
    }

    private String construirMensaje(ReportePerfilCreadoEvent evento) {
        String descripcion = evento.descripcion() != null
                ? evento.descripcion()
                : "Sin descripción";

        return "Nuevo reporte de " + evento.nombreUsuarioAutor()
                + " (" + evento.nombrePerfilAutor() + ") contra el perfil '"
                + evento.nombrePerfilReportado() + "'. Motivo: "
                + evento.motivo().getDescripcion()
                + ". Descripción: " + descripcion + ".";
    }
}
