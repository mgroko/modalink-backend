package org.mgroko.backend.invitaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.servicio.EvaluarDisponibilidadActividadService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.invitaciones.dto.InvitacionGestionadaResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.evento.DisponibilidadModificadaEvent;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoActividadModificadoEvent;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-28: aceptar una invitación general o por actividad.
 */
@Service
public class AceptarInvitacionService {

    private static final String ROL_MIEMBRO = "MIEMBRO";

    private final PerfilRepository perfilRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RolProyectoRepository rolProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final EvaluarDisponibilidadActividadService evaluarDisponibilidadActividadService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final NotificacionService notificacionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public AceptarInvitacionService(
            PerfilRepository perfilRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RolProyectoRepository rolProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            EvaluarDisponibilidadActividadService evaluarDisponibilidadActividadService,
            ReprogramarProyectoService reprogramarProyectoService,
            NotificacionService notificacionService,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.evaluarDisponibilidadActividadService = evaluarDisponibilidadActividadService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.notificacionService = notificacionService;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public InvitacionGestionadaResponse aceptarGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idInvitacion) {

        Perfil perfil = perfilActivo(idUsuario, idPerfilActivo);

        InvitacionGral inicial = invitacionGralRepository.findById(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        Long idRequerimientoGral = inicial.getRequerimientoGral() != null
                ? inicial.getRequerimientoGral().getIdRequerimientoGral()
                : null;

        RequerimientoGralProyecto requerimiento = null;
        if (idRequerimientoGral != null) {
            requerimiento = requerimientoGralProyectoRepository
                    .findByIdParaGestion(idRequerimientoGral)
                    .orElseThrow(() -> new RequerimientoNoEncontradoException(
                            "El requerimiento general " + idRequerimientoGral + " no existe."));
        }

        InvitacionGral invitacion = invitacionGralRepository.findByIdForUpdate(idInvitacion)
                .orElseThrow(InvitacionNoEncontradaException::new);

        Proyecto proyecto = invitacion.getProyecto();
        validarInvitacionGeneral(invitacion, perfil, proyecto);

        if (requerimiento != null) {
            validarRequerimientoGeneral(requerimiento, proyecto, perfil);
        }

        if (miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        proyecto.getIdProyecto(), perfil.getIdPerfil(),
                        EstadoParticipacion.ACTIVO)) {
            throw new InvitacionNoPermitidaException(
                    "El perfil ya es miembro activo de este proyecto.");
        }

        MiembroProyecto miembro = crearMiembro(
                proyecto, perfil, requerimiento, invitacion.getIdTycAceptado());

        if (requerimiento != null) {
            eventPublisher.publishEvent(new CupoRequerimientoGralModificadoEvent(
                    requerimiento.getIdRequerimientoGral()));
        }

        invitacion.setEstado(EstadoSolicitud.ACEPTADA);
        invitacionGralRepository.saveAndFlush(invitacion);

        notificarDirectores(
                proyecto,
                NotificacionService.TIPO_INVITACION_ACEPTADA,
                "El perfil '" + perfil.getNombreArtistico()
                        + "' aceptó la invitación al proyecto '"
                        + proyecto.getNombre() + "'.",
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                invitacion.getIdInvitacionGral());

        eliminarPendientesGeneralesSiCorresponde(requerimiento);

        return new InvitacionGestionadaResponse(
                invitacion.getIdInvitacionGral(),
                TipoInvitacion.GENERAL,
                invitacion.getEstado(),
                proyecto.getIdProyecto(),
                perfil.getIdPerfil(),
                miembro.getIdMiembro(),
                null,
                null,
                List.of());
    }

    @Transactional
    @AuditarOperacion
    public InvitacionGestionadaResponse aceptarActividad(
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

        Proyecto proyecto = requerimiento.getActividad().getPlanificacion().getProyecto();
        validarInvitacionActividad(invitacion, perfil, proyecto, requerimiento);

        var actividad = requerimiento.getActividad();
        if (actividad.getEstado() != EstadoActividad.PENDIENTE
                && actividad.getEstado() != EstadoActividad.EN_CURSO) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se puede aceptar invitaciones de actividades Pendientes o En curso.");
        }

        if (cupoRequerimientoService.calcularActividad(requerimiento).satisfecho()) {
            throw new InvitacionNoPermitidaException(
                    "El requerimiento de actividad ya está satisfecho.");
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findMiembroActivoConRol(proyecto.getIdProyecto(), perfil.getIdPerfil())
                .orElse(null);

        if (miembro != null
                && RolProyectoUsuario.desde(miembro.getRolProyecto())
                        == RolProyectoUsuario.DIRECTOR) {
            throw new InvitacionNoPermitidaException(
                    "Un director no puede aceptar una invitación de actividad.");
        }

        if (miembro != null
                && asignacionActividadRepository.existsByMiembro_IdMiembroAndActividad_IdActividad(
                        miembro.getIdMiembro(), actividad.getIdActividad())) {
            throw new InvitacionNoPermitidaException(
                    "El perfil ya está asignado a esta actividad.");
        }

        if (asignacionActividadRepository.existsAsignacionActivaDeUsuarioEnActividad(
                actividad.getIdActividad(), perfil.getUsuario().getIdUsuario())) {
            throw new InvitacionNoPermitidaException(
                    "El usuario ya está asignado a esta actividad con otro perfil.");
        }

        Set<Long> idsUsuarios = new LinkedHashSet<>();
        idsUsuarios.add(perfil.getUsuario().getIdUsuario());
        asignacionActividadRepository.findActivasByActividadId(actividad.getIdActividad())
                .forEach(asignacion -> {
                    if (asignacion.getMiembro() != null
                            && asignacion.getMiembro().getPerfil() != null
                            && asignacion.getMiembro().getPerfil().getUsuario() != null) {
                        idsUsuarios.add(asignacion.getMiembro().getPerfil()
                                .getUsuario().getIdUsuario());
                    }
                });

        ResultadoDisponibilidad disponibilidad =
                evaluarDisponibilidadActividadService.evaluar(
                        proyecto, actividad, List.copyOf(idsUsuarios));

        if (!disponibilidad.disponible()) {
            throw new InvitacionNoPermitidaException(
                    "No hay disponibilidad suficiente dentro del rango factible de la actividad.",
                    disponibilidad.codigoMotivo());
        }

        if (miembro == null) {
            miembro = crearMiembro(
                    proyecto, perfil, null, invitacion.getIdTycAceptado());
        } else if (miembro.getIdTycAceptado() == null
                && invitacion.getIdTycAceptado() != null) {
            miembro.setIdTycAceptado(invitacion.getIdTycAceptado());
            miembro = miembroProyectoRepository.saveAndFlush(miembro);
        }

        if (proyecto.getEstado() != null
                && proyecto.getEstado().permiteReprogramacion()
                && actividad.getEstado() == EstadoActividad.PENDIENTE) {
            reprogramarProyectoService.reprogramarActividad(
                    proyecto.getIdProyecto(),
                    actividad.getIdActividad(),
                    MotivoReprogramacion.ASIGNACION,
                    Map.of(actividad.getIdActividad(),
                            List.of(perfil.getUsuario().getIdUsuario())));
        }

        AsignacionActividad asignacion;
        try {
            asignacion = asignacionActividadRepository.saveAndFlush(
                    AsignacionActividad.builder()
                            .miembro(miembro)
                            .actividad(actividad)
                            .requerimientoActividad(requerimiento)
                            .build());
        } catch (DataIntegrityViolationException ex) {
            throw new InvitacionNoPermitidaException(
                    "El perfil ya está asignado a esta actividad.");
        }

        if (requerimiento != null) {
            eventPublisher.publishEvent(new CupoRequerimientoActividadModificadoEvent(
                    requerimiento.getIdRequerimiento()));
        }

        invitacion.setEstado(EstadoSolicitud.ACEPTADA);
        invitacion = invitacionActividadRepository.saveAndFlush(invitacion);

        if (proyecto.getEstado() != null && proyecto.getEstado().permiteReprogramacion()) {
            eventPublisher.publishEvent(new DisponibilidadModificadaEvent(
                    perfil.getUsuario().getIdUsuario(),
                    MotivoReprogramacion.ASIGNACION,
                    proyecto.getIdProyecto()));
        }

        notificarDirectores(
                proyecto,
                NotificacionService.TIPO_INVITACION_ACEPTADA,
                "El perfil '" + perfil.getNombreArtistico()
                        + "' aceptó la invitación a la actividad '"
                        + actividad.getNombre() + "' del proyecto '"
                        + proyecto.getNombre() + "'.",
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                invitacion.getIdInvitacion());

        eliminarPendientesActividadSiCorresponde(requerimiento);

        return new InvitacionGestionadaResponse(
                invitacion.getIdInvitacion(),
                TipoInvitacion.ACTIVIDAD,
                invitacion.getEstado(),
                proyecto.getIdProyecto(),
                perfil.getIdPerfil(),
                miembro.getIdMiembro(),
                asignacion.getIdAsignacionAct(),
                disponibilidad,
                List.of());
    }

    private void validarInvitacionGeneral(
            InvitacionGral invitacion,
            Perfil perfil,
            Proyecto proyecto) {

        validarDestinatario(invitacion.getPerfilDestinatario(), perfil);
        validarEstadoPendiente(invitacion.getEstado());
        validarProyecto(proyecto);
    }

    private void validarInvitacionActividad(
            InvitacionActividad invitacion,
            Perfil perfil,
            Proyecto proyecto,
            RequerimientoActividad requerimiento) {

        validarDestinatario(invitacion.getPerfilDestinatario(), perfil);
        validarEstadoPendiente(invitacion.getEstado());
        validarProyecto(proyecto);

        if (!requerimiento.getActividad().getPlanificacion().getProyecto()
                .getIdProyecto().equals(proyecto.getIdProyecto())) {
            throw new InvitacionNoEncontradaException();
        }
    }

    private void validarRequerimientoGeneral(
            RequerimientoGralProyecto requerimiento,
            Proyecto proyecto,
            Perfil perfil) {

        if (!requerimiento.getProyecto().getIdProyecto().equals(proyecto.getIdProyecto())) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        if (perfil.getProfesion() == null
                || requerimiento.getProfesion() == null
                || !perfil.getProfesion().getIdProfesion()
                        .equals(requerimiento.getProfesion().getIdProfesion())) {
            throw new InvitacionNoPermitidaException(
                    "El perfil no tiene la profesión requerida por el requerimiento general.");
        }

        if (cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            throw new InvitacionNoPermitidaException(
                    "El requerimiento general ya está satisfecho.");
        }
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
                    "Sólo se pueden aceptar invitaciones pendientes.",
                    "INVITACION_NO_PENDIENTE");
        }
    }

    private void validarProyecto(Proyecto proyecto) {
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new InvitacionNoPermitidaException(
                    "El proyecto no permite aceptar invitaciones en estado "
                            + proyecto.getEstado().getNombre() + ".");
        }
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

    private MiembroProyecto crearMiembro(
            Proyecto proyecto,
            Perfil perfil,
            RequerimientoGralProyecto requerimientoGral,
            Long idTycAceptado) {

        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo(ROL_MIEMBRO)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_MIEMBRO));

        try {
            return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                    .proyecto(proyecto)
                    .perfil(perfil)
                    .rolProyecto(rolMiembro)
                    .estadoParticipacion(EstadoParticipacion.ACTIVO)
                    .fechaAlta(LocalDateTime.now(reloj))
                    .requerimientoGral(requerimientoGral)
                    .idTycAceptado(idTycAceptado)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new InvitacionNoPermitidaException(
                    "El perfil ya es miembro activo de este proyecto.");
        }
    }

    private void eliminarPendientesGeneralesSiCorresponde(
            RequerimientoGralProyecto requerimiento) {

        if (requerimiento == null) {
            return;
        }

        if (!cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            return;
        }

        List<InvitacionGral> pendientes = invitacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(
                        requerimiento.getIdRequerimientoGral());
        if (!pendientes.isEmpty()) {
            invitacionGralRepository.deleteAll(pendientes);
            invitacionGralRepository.flush();
        }
    }

    private void eliminarPendientesActividadSiCorresponde(
            RequerimientoActividad requerimiento) {

        if (!cupoRequerimientoService.calcularActividad(requerimiento).satisfecho()) {
            return;
        }

        List<InvitacionActividad> pendientes = invitacionActividadRepository
                .findPendientesPorRequerimientoParaActualizar(
                        requerimiento.getIdRequerimiento());
        if (!pendientes.isEmpty()) {
            invitacionActividadRepository.deleteAll(pendientes);
            invitacionActividadRepository.flush();
        }
    }

    private void notificarDirectores(
            Proyecto proyecto,
            String tipo,
            String mensaje,
            String entidadTipo,
            Long entidadId) {

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(proyecto.getIdProyecto()));

        if (directores.isEmpty()) {
            return;
        }

        List<Notificacion> notificaciones = new java.util.ArrayList<>();
        for (MiembroProyecto director : directores) {
            Perfil perfilDirector = director.getPerfil();
            if (perfilDirector == null || perfilDirector.getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    perfilDirector.getUsuario(),
                    perfilDirector,
                    tipo,
                    mensaje,
                    entidadTipo,
                    entidadId,
                    proyecto.getIdProyecto()));
        }
        notificacionService.registrarTodas(notificaciones);
    }
}
