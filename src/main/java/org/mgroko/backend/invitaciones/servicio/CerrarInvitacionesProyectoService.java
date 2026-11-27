package org.mgroko.backend.invitaciones.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra las invitaciones pendientes de un proyecto que deja de estar activo.
 *
 * <p>La invitación no se elimina: pasa a {@code RECHAZADA} y conserva
 * {@code fecha_rechazo} para el historial y la auditoría. Se usa al finalizar
 * y, opcionalmente, al cancelar un proyecto.</p>
 */
@Service
public class CerrarInvitacionesProyectoService {

    public enum Motivo {
        FINALIZACION("finalizado"),
        CANCELACION("cancelado");

        private final String participio;

        Motivo(String participio) {
            this.participio = participio;
        }

        String participio() {
            return participio;
        }
    }

    public record Resultado(int generales, int actividades) {
        public int total() {
            return generales + actividades;
        }
    }

    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final NotificacionService notificacionService;

    public CerrarInvitacionesProyectoService(
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            NotificacionService notificacionService) {
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Resultado cerrarPendientes(
            Proyecto proyecto,
            Motivo motivo,
            LocalDateTime fechaCierre) {

        if (proyecto == null || proyecto.getIdProyecto() == null) {
            return new Resultado(0, 0);
        }

        LocalDateTime fecha = fechaCierre != null
                ? fechaCierre
                : LocalDateTime.now();

        List<Notificacion> notificaciones = new ArrayList<>();
        List<InvitacionGral> generales = invitacionGralRepository
                .findPendientesDeProyectoConDestinatario(proyecto.getIdProyecto());
        List<InvitacionActividad> actividades = invitacionActividadRepository
                .findPendientesDeProyectoConDestinatario(proyecto.getIdProyecto());

        for (InvitacionGral invitacion : generales) {
            invitacion.setEstado(EstadoSolicitud.RECHAZADA);
            invitacion.setFechaRechazo(fecha);
            notificacionService.eliminarPorEntidad(
                    NotificacionService.TIPO_INVITACION_RECIBIDA,
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                    invitacion.getIdInvitacionGral());
            agregarNotificacion(
                    notificaciones,
                    invitacion.getPerfilDestinatario(),
                    "La invitación al proyecto '" + proyecto.getNombre()
                            + "' fue rechazada porque el proyecto fue "
                            + motivo.participio() + ".",
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                    invitacion.getIdInvitacionGral(),
                    proyecto.getIdProyecto());
        }

        for (InvitacionActividad invitacion : actividades) {
            invitacion.setEstado(EstadoSolicitud.RECHAZADA);
            invitacion.setFechaRechazo(fecha);
            notificacionService.eliminarPorEntidad(
                    NotificacionService.TIPO_INVITACION_RECIBIDA,
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                    invitacion.getIdInvitacion());
            agregarNotificacion(
                    notificaciones,
                    invitacion.getPerfilDestinatario(),
                    "La invitación a una actividad del proyecto '"
                            + proyecto.getNombre()
                            + "' fue rechazada porque el proyecto fue "
                            + motivo.participio() + ".",
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                    invitacion.getIdInvitacion(),
                    proyecto.getIdProyecto());
        }

        invitacionGralRepository.saveAll(generales);
        invitacionActividadRepository.saveAll(actividades);
        if (!notificaciones.isEmpty()) {
            notificacionService.registrarTodas(notificaciones);
        }

        return new Resultado(generales.size(), actividades.size());
    }

    private void agregarNotificacion(
            List<Notificacion> notificaciones,
            Perfil destinatario,
            String mensaje,
            String entidadTipo,
            Long entidadId,
            Long idProyecto) {

        if (destinatario == null) {
            return;
        }
        Usuario usuario = destinatario.getUsuario();
        if (usuario == null) {
            return;
        }
        notificaciones.add(notificacionService.crear(
                usuario,
                destinatario,
                NotificacionService.TIPO_INVITACION_RECHAZADA,
                mensaje,
                entidadTipo,
                entidadId,
                idProyecto));
    }
}
