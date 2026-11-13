package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rechaza automáticamente las postulaciones e invitaciones pendientes cuando
 * el cupo de un requerimiento queda cubierto.
 *
 * <p>La regla se ejecuta dentro de la misma transacción que cubrió la vacante.
 * Sólo se rechazan filas {@code PENDIENTE}, por lo que repetir la operación es
 * idempotente. Antes de notificar se verifica que no exista ya una notificación
 * para la misma entidad, de modo que un reintento no duplique avisos.</p>
 *
 * <p>El rechazo es lógico: la fila conserva su historial. Una postulación
 * rechazada no bloquea un nuevo intento si el cupo vuelve a liberarse.</p>
 */
@Service
public class RechazoAutomaticoCupoService {

    private static final String MOTIVO_CUPO_CUBIERTO = "porque el cupo ya fue cubierto";

    private final CupoRequerimientoService cupoRequerimientoService;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;
    private final Clock reloj;

    public RechazoAutomaticoCupoService(
            CupoRequerimientoService cupoRequerimientoService,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            PostulacionGralRepository postulacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            InvitacionGralRepository invitacionGralRepository,
            NotificacionService notificacionService,
            NotificacionRepository notificacionRepository,
            Clock reloj) {
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
        this.reloj = reloj;
    }

    /**
     * Rechaza postulaciones e invitaciones pendientes de un requerimiento de
     * actividad si el cupo quedó cubierto.
     *
     * @return cantidad de filas rechazadas
     */
    @Transactional
    public int rechazarPendientesSiCupoCompletoActividad(Long idRequerimiento) {
        if (idRequerimiento == null) {
            return 0;
        }

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdConActividadYProyecto(idRequerimiento)
                .orElse(null);

        if (requerimiento == null
                || !cupoRequerimientoService.calcularActividad(requerimiento).satisfecho()) {
            return 0;
        }

        List<PostulacionActividad> postulaciones = postulacionActividadRepository
                .findPendientesPorRequerimientoParaActualizar(idRequerimiento);
        List<InvitacionActividad> invitaciones = invitacionActividadRepository
                .findPendientesPorRequerimientoParaActualizar(idRequerimiento);

        return rechazarActividad(postulaciones, invitaciones);
    }

    /**
     * Rechaza postulaciones e invitaciones pendientes de un requerimiento
     * general si el cupo quedó cubierto.
     *
     * @return cantidad de filas rechazadas
     */
    @Transactional
    public int rechazarPendientesSiCupoCompletoGeneral(Long idRequerimientoGral) {
        if (idRequerimientoGral == null) {
            return 0;
        }

        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByIdConProyecto(idRequerimientoGral)
                .orElse(null);

        if (requerimiento == null
                || !cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            return 0;
        }

        List<PostulacionGral> postulaciones = postulacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(idRequerimientoGral);
        List<InvitacionGral> invitaciones = invitacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(idRequerimientoGral);

        return rechazarGeneral(postulaciones, invitaciones);
    }

    private int rechazarActividad(
            List<PostulacionActividad> postulaciones,
            List<InvitacionActividad> invitaciones) {

        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Notificacion> notificaciones = new ArrayList<>();

        for (PostulacionActividad postulacion : postulaciones) {
            postulacion.setEstado(EstadoSolicitud.RECHAZADA);
            agregarNotificacionPostulacionActividad(notificaciones, postulacion);
        }

        for (InvitacionActividad invitacion : invitaciones) {
            invitacion.setEstado(EstadoSolicitud.RECHAZADA);
            invitacion.setFechaRechazo(ahora);
            agregarNotificacionInvitacionActividad(notificaciones, invitacion);
        }

        if (!postulaciones.isEmpty()) {
            postulacionActividadRepository.saveAllAndFlush(postulaciones);
        }
        if (!invitaciones.isEmpty()) {
            invitacionActividadRepository.saveAllAndFlush(invitaciones);
        }
        notificacionService.registrarTodas(notificaciones);

        return postulaciones.size() + invitaciones.size();
    }

    private int rechazarGeneral(
            List<PostulacionGral> postulaciones,
            List<InvitacionGral> invitaciones) {

        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Notificacion> notificaciones = new ArrayList<>();

        for (PostulacionGral postulacion : postulaciones) {
            postulacion.setEstado(EstadoSolicitud.RECHAZADA);
            agregarNotificacionPostulacionGeneral(notificaciones, postulacion);
        }

        for (InvitacionGral invitacion : invitaciones) {
            invitacion.setEstado(EstadoSolicitud.RECHAZADA);
            invitacion.setFechaRechazo(ahora);
            agregarNotificacionInvitacionGeneral(notificaciones, invitacion);
        }

        if (!postulaciones.isEmpty()) {
            postulacionGralRepository.saveAllAndFlush(postulaciones);
        }
        if (!invitaciones.isEmpty()) {
            invitacionGralRepository.saveAllAndFlush(invitaciones);
        }
        notificacionService.registrarTodas(notificaciones);

        return postulaciones.size() + invitaciones.size();
    }

    private void agregarNotificacionPostulacionActividad(
            List<Notificacion> notificaciones,
            PostulacionActividad postulacion) {

        Perfil perfil = postulacion.getPerfil();
        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Long idProyecto = postulacion.getRequerimiento().getActividad()
                .getPlanificacion().getProyecto().getIdProyecto();

        if (!yaNotificada(
                perfil,
                NotificacionService.TIPO_POSTULACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_POSTULACION_ACTIVIDAD,
                postulacion.getIdPostulacion())) {

            String mensaje = "Tu postulación a la actividad '"
                    + postulacion.getRequerimiento().getActividad().getNombre()
                    + "' del proyecto '"
                    + postulacion.getRequerimiento().getActividad()
                            .getPlanificacion().getProyecto().getNombre()
                    + "' fue rechazada " + MOTIVO_CUPO_CUBIERTO + ".";

            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_POSTULACION_RECHAZADA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_POSTULACION_ACTIVIDAD,
                    postulacion.getIdPostulacion(),
                    idProyecto));
        }
    }

    private void agregarNotificacionPostulacionGeneral(
            List<Notificacion> notificaciones,
            PostulacionGral postulacion) {

        Perfil perfil = postulacion.getPerfil();
        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Long idProyecto = postulacion.getRequerimientoGral().getProyecto().getIdProyecto();

        if (!yaNotificada(
                perfil,
                NotificacionService.TIPO_POSTULACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_POSTULACION_GRAL,
                postulacion.getIdPostulacionGral())) {

            String mensaje = "Tu postulación al proyecto '"
                    + postulacion.getRequerimientoGral().getProyecto().getNombre()
                    + "' fue rechazada " + MOTIVO_CUPO_CUBIERTO + ".";

            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_POSTULACION_RECHAZADA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_POSTULACION_GRAL,
                    postulacion.getIdPostulacionGral(),
                    idProyecto));
        }
    }

    private void agregarNotificacionInvitacionActividad(
            List<Notificacion> notificaciones,
            InvitacionActividad invitacion) {

        Perfil perfil = invitacion.getPerfilDestinatario();
        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Long idProyecto = invitacion.getRequerimiento().getActividad()
                .getPlanificacion().getProyecto().getIdProyecto();

        if (!yaNotificada(
                perfil,
                NotificacionService.TIPO_INVITACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                invitacion.getIdInvitacion())) {

            String mensaje = "Tu invitación a la actividad '"
                    + invitacion.getRequerimiento().getActividad().getNombre()
                    + "' del proyecto '"
                    + invitacion.getRequerimiento().getActividad()
                            .getPlanificacion().getProyecto().getNombre()
                    + "' fue rechazada " + MOTIVO_CUPO_CUBIERTO + ".";

            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_INVITACION_RECHAZADA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                    invitacion.getIdInvitacion(),
                    idProyecto));
        }
    }

    private void agregarNotificacionInvitacionGeneral(
            List<Notificacion> notificaciones,
            InvitacionGral invitacion) {

        Perfil perfil = invitacion.getPerfilDestinatario();
        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Long idProyecto = invitacion.getProyecto().getIdProyecto();

        if (!yaNotificada(
                perfil,
                NotificacionService.TIPO_INVITACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                invitacion.getIdInvitacionGral())) {

            String mensaje = "Tu invitación al proyecto '"
                    + invitacion.getProyecto().getNombre()
                    + "' fue rechazada " + MOTIVO_CUPO_CUBIERTO + ".";

            notificaciones.add(notificacionService.crear(
                    perfil.getUsuario(),
                    perfil,
                    NotificacionService.TIPO_INVITACION_RECHAZADA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                    invitacion.getIdInvitacionGral(),
                    idProyecto));
        }
    }

    private boolean yaNotificada(
            Perfil perfil,
            String tipo,
            String entidadTipo,
            Long entidadId) {

        return perfil != null
                && perfil.getUsuario() != null
                && entidadId != null
                && notificacionRepository
                        .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadId(
                                perfil.getUsuario().getIdUsuario(),
                                tipo,
                                entidadTipo,
                                entidadId);
    }
}
