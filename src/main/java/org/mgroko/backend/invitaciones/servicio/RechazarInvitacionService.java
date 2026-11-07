package org.mgroko.backend.invitaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.invitaciones.dto.InvitacionGestionadaResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-28: rechazar una invitación general o por actividad.
 */
@Service
public class RechazarInvitacionService {

    private final PerfilRepository perfilRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;

    public RechazarInvitacionService(
            PerfilRepository perfilRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService) {
        this.perfilRepository = perfilRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    @AuditarOperacion
    public InvitacionGestionadaResponse rechazarGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idInvitacion) {

        Perfil perfil = perfilActivo(idUsuario, idPerfilActivo);

        InvitacionGral inicial = invitacionGralRepository.findById(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        if (inicial.getRequerimientoGral() != null) {
            Long idRequerimientoGral =
                    inicial.getRequerimientoGral().getIdRequerimientoGral();
            requerimientoGralProyectoRepository.findByIdParaGestion(idRequerimientoGral)
                    .orElseThrow(() -> new RequerimientoNoEncontradoException(
                            "El requerimiento general " + idRequerimientoGral + " no existe."));
        }

        InvitacionGral invitacion = invitacionGralRepository.findByIdForUpdate(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        validarDestinatario(invitacion.getPerfilDestinatario(), perfil);
        validarEstadoPendiente(invitacion.getEstado());
        Proyecto proyecto = invitacion.getProyecto();
        validarProyecto(proyecto);

        invitacion.setEstado(EstadoSolicitud.RECHAZADA);
        invitacionGralRepository.saveAndFlush(invitacion);

        notificarDirectores(
                proyecto,
                "El perfil '" + perfil.getNombreArtistico()
                        + "' rechazó la invitación al proyecto '"
                        + proyecto.getNombre() + "'.",
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                invitacion.getIdInvitacionGral());

        return new InvitacionGestionadaResponse(
                invitacion.getIdInvitacionGral(),
                TipoInvitacion.GENERAL,
                invitacion.getEstado(),
                proyecto.getIdProyecto(),
                perfil.getIdPerfil(),
                null,
                null,
                null,
                List.of());
    }

    @Transactional
    @AuditarOperacion
    public InvitacionGestionadaResponse rechazarActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idInvitacion) {

        Perfil perfil = perfilActivo(idUsuario, idPerfilActivo);

        InvitacionActividad inicial = invitacionActividadRepository.findById(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        Long idRequerimiento = inicial.getRequerimiento().getIdRequerimiento();
        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdParaGestion(idRequerimiento)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad " + idRequerimiento + " no existe."));

        InvitacionActividad invitacion = invitacionActividadRepository
                .findByIdForUpdate(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        validarDestinatario(invitacion.getPerfilDestinatario(), perfil);
        validarEstadoPendiente(invitacion.getEstado());
        Proyecto proyecto = requerimiento.getActividad().getPlanificacion().getProyecto();
        validarProyecto(proyecto);

        invitacion.setEstado(EstadoSolicitud.RECHAZADA);
        invitacionActividadRepository.saveAndFlush(invitacion);

        notificarDirectores(
                proyecto,
                "El perfil '" + perfil.getNombreArtistico()
                        + "' rechazó la invitación a la actividad '"
                        + requerimiento.getActividad().getNombre()
                        + "' del proyecto '" + proyecto.getNombre() + "'.",
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                invitacion.getIdInvitacion());

        return new InvitacionGestionadaResponse(
                invitacion.getIdInvitacion(),
                TipoInvitacion.ACTIVIDAD,
                invitacion.getEstado(),
                proyecto.getIdProyecto(),
                perfil.getIdPerfil(),
                null,
                null,
                null,
                List.of());
    }

    private Perfil perfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException("El usuario del perfil no se encuentra activo.");
        }

        return perfil;
    }

    private void validarDestinatario(Perfil destinatario, Perfil perfilActivo) {
        if (destinatario == null
                || destinatario.getIdPerfil() == null
                || !destinatario.getIdPerfil().equals(perfilActivo.getIdPerfil())) {
            throw new AccesoDenegadoProyectoException(
                    "La invitación no pertenece al perfil activo.");
        }
    }

    private void validarEstadoPendiente(EstadoSolicitud estado) {
        if (estado != EstadoSolicitud.PENDIENTE) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se pueden rechazar invitaciones pendientes.",
                    "INVITACION_NO_PENDIENTE");
        }
    }

    private void validarProyecto(Proyecto proyecto) {
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new InvitacionNoPermitidaException(
                    "El proyecto no permite rechazar invitaciones en estado "
                            + proyecto.getEstado().getNombre() + ".");
        }
    }

    private void notificarDirectores(
            Proyecto proyecto,
            String mensaje,
            String entidadTipo,
            Long entidadId) {

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(proyecto.getIdProyecto()));

        if (directores.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new ArrayList<>();
        for (MiembroProyecto director : directores) {
            Perfil perfilDirector = director.getPerfil();
            if (perfilDirector == null || perfilDirector.getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfilDirector.getUsuario(),
                    perfilDirector,
                    NotificacionService.TIPO_INVITACION_RECHAZADA,
                    mensaje,
                    entidadTipo,
                    entidadId,
                    proyecto.getIdProyecto()));
        }
        notificacionService.registrarTodas(notificaciones);
    }
}
