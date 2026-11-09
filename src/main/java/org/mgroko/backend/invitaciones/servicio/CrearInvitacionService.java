package org.mgroko.backend.invitaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.invitaciones.mapper.InvitacionMapper;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-31: alta de invitaciones generales y por actividad.
 *
 * <p>La invitación general se asocia directamente al proyecto. La invitación
 * por actividad se asocia al requerimiento y deriva el proyecto desde la
 * actividad. En ambos casos la invitación nace en estado PENDIENTE y notifica
 * al perfil destinatario.</p>
 */
@Service
public class CrearInvitacionService {

    public static final String CODIGO_INVITACION_DUPLICADA = "INVITACION_DUPLICADA";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final GestionTerminosCondicionesService gestionTerminosCondicionesService;
    private final Clock reloj;

    public CrearInvitacionService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            GestionTerminosCondicionesService gestionTerminosCondicionesService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.gestionTerminosCondicionesService = gestionTerminosCondicionesService;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public InvitacionResponse invitarAProyecto(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CrearInvitacionGralRequest request) {

        Perfil remitente = perfilActivo(idUsuario, idPerfilActivo);
        Proyecto proyecto = proyectoParaInvitar(idProyecto, idPerfilActivo);
        Perfil destinatario = perfilDestinatario(request.idPerfilDestinatario());

        RequerimientoGralProyecto requerimientoGral = null;
        if (request.idRequerimientoGral() != null) {
            requerimientoGral = requerimientoGralProyectoRepository
                    .findByIdParaGestion(request.idRequerimientoGral())
                    .orElseThrow(() -> new RequerimientoNoEncontradoException(
                            "El requerimiento general "
                                    + request.idRequerimientoGral()
                                    + " no existe."));
            validarRequerimientoGral(requerimientoGral, proyecto, destinatario);
        }

        validarPuedeRecibirInvitacion(idProyecto, remitente, destinatario);
        validarSinInvitacionPendiente(idProyecto, destinatario);

        Long idTycAceptado = gestionTerminosCondicionesService.validarAceptacion(
                destinatario.getIdPerfil(), request.aceptacionTerminos());

        InvitacionGral invitacion = InvitacionGral.builder()
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .proyecto(proyecto)
                .requerimientoGral(requerimientoGral)
                .mensaje(normalizarMensaje(request.mensaje()))
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now(reloj))
                .idTycAceptado(idTycAceptado)
                .build();

        try {
            invitacion = invitacionGralRepository.saveAndFlush(invitacion);
        } catch (DataIntegrityViolationException ex) {
            throw invitacionDuplicada();
        }

        notificarInvitacion(
                destinatario,
                proyecto,
                null,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL,
                invitacion.getIdInvitacionGral());

        return InvitacionMapper.toResponse(invitacion);
    }

    @Transactional
    @AuditarOperacion
    public InvitacionResponse invitarAProyectoActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CrearInvitacionActividadRequest request) {

        Perfil remitente = perfilActivo(idUsuario, idPerfilActivo);
        Proyecto proyecto = proyectoParaInvitar(idProyecto, idPerfilActivo);
        Perfil destinatario = perfilDestinatario(request.idPerfilDestinatario());

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdParaGestion(request.idRequerimientoActividad())
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad "
                                + request.idRequerimientoActividad()
                                + " no existe."));

        validarRequerimiento(requerimiento, proyecto, destinatario);
        validarPuedeRecibirInvitacion(idProyecto, remitente, destinatario);
        validarSinInvitacionPendiente(idProyecto, destinatario);

        Long idTycAceptado = gestionTerminosCondicionesService.validarAceptacion(
                destinatario.getIdPerfil(), request.aceptacionTerminos());

        InvitacionActividad invitacion = InvitacionActividad.builder()
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .requerimiento(requerimiento)
                .mensaje(normalizarMensaje(request.mensaje()))
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now(reloj))
                .idTycAceptado(idTycAceptado)
                .build();

        try {
            invitacion = invitacionActividadRepository.saveAndFlush(invitacion);
        } catch (DataIntegrityViolationException ex) {
            throw invitacionDuplicada();
        }

        notificarInvitacion(
                destinatario,
                proyecto,
                requerimiento,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD,
                invitacion.getIdInvitacion());

        return InvitacionMapper.toResponse(invitacion);
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

    private Proyecto proyectoParaInvitar(Long idProyecto, Long idPerfilActivo) {
        // El bloqueo serializa las invitaciones de un mismo proyecto y evita
        // duplicados entre invitaciones generales y de actividad.
        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, ProyectoSecurityService.PERMISO_INVITAR_MIEMBRO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se puede invitar en proyectos Publicados o Confirmados.");
        }

        return proyecto;
    }

    private Perfil perfilDestinatario(Long idPerfilDestinatario) {
        Perfil perfil = perfilRepository.findByIdConProfesion(idPerfilDestinatario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil destinatario no se encuentra activo.");
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException(
                    "El usuario del perfil destinatario no se encuentra activo.");
        }

        return perfil;
    }

    private void validarPuedeRecibirInvitacion(
            Long idProyecto,
            Perfil remitente,
            Perfil destinatario) {

        if (remitente.getIdPerfil().equals(destinatario.getIdPerfil())) {
            throw new InvitacionNoPermitidaException(
                    "No podés enviarte una invitación a vos mismo.");
        }

        if (miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, destinatario.getIdPerfil())
                .isPresent()) {
            throw new InvitacionNoPermitidaException(
                    "El perfil ya es miembro activo de este proyecto.");
        }
    }

    private void validarRequerimiento(
            RequerimientoActividad requerimiento,
            Proyecto proyecto,
            Perfil destinatario) {

        if (!requerimiento.getActividad().getPlanificacion().getProyecto()
                .getIdProyecto().equals(proyecto.getIdProyecto())) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        EstadoActividad estado = requerimiento.getActividad().getEstado();
        if (estado != EstadoActividad.PENDIENTE && estado != EstadoActividad.EN_CURSO) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se puede invitar a actividades Pendientes o En curso.");
        }

        if (destinatario.getProfesion() == null
                || requerimiento.getProfesion() == null
                || !destinatario.getProfesion().getIdProfesion()
                        .equals(requerimiento.getProfesion().getIdProfesion())) {
            throw new InvitacionNoPermitidaException(
                    "El perfil destinatario no tiene la profesión requerida por la actividad.");
        }

        if (cupoRequerimientoService.calcularActividad(requerimiento).satisfecho()) {
            throw new InvitacionNoPermitidaException(
                    "El requerimiento de actividad ya está satisfecho.");
        }
    }

    private void validarRequerimientoGral(
            RequerimientoGralProyecto requerimiento,
            Proyecto proyecto,
            Perfil destinatario) {

        if (!requerimiento.getProyecto().getIdProyecto().equals(proyecto.getIdProyecto())) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        if (destinatario.getProfesion() == null
                || requerimiento.getProfesion() == null
                || !destinatario.getProfesion().getIdProfesion()
                        .equals(requerimiento.getProfesion().getIdProfesion())) {
            throw new InvitacionNoPermitidaException(
                    "El perfil destinatario no tiene la profesión requerida por el requerimiento general.");
        }

        if (cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho()) {
            throw new InvitacionNoPermitidaException(
                    "El requerimiento general ya está satisfecho.");
        }
    }

    private void validarSinInvitacionPendiente(Long idProyecto, Perfil destinatario) {
        boolean general = invitacionGralRepository
                .existsByProyecto_IdProyectoAndPerfilDestinatario_IdPerfilAndEstado(
                        idProyecto, destinatario.getIdPerfil(), EstadoSolicitud.PENDIENTE);

        boolean actividad = invitacionActividadRepository
                .existsDeProyectoYDestinatarioEnEstado(
                        idProyecto, destinatario.getIdPerfil(), EstadoSolicitud.PENDIENTE);

        if (general || actividad) {
            throw invitacionDuplicada();
        }
    }

    private InvitacionNoPermitidaException invitacionDuplicada() {
        return new InvitacionNoPermitidaException(
                "El perfil ya tiene una invitación activa para este proyecto.",
                CODIGO_INVITACION_DUPLICADA);
    }

    private String normalizarMensaje(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        String normalizado = mensaje.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private void notificarInvitacion(
            Perfil destinatario,
            Proyecto proyecto,
            RequerimientoActividad requerimiento,
            String entidadTipo,
            Long entidadId) {

        String mensaje = requerimiento == null
                ? "Recibiste una invitación para participar del proyecto '"
                        + proyecto.getNombre() + "'."
                : "Recibiste una invitación para participar de la actividad '"
                        + requerimiento.getActividad().getNombre()
                        + "' del proyecto '" + proyecto.getNombre() + "'.";

        notificacionService.registrarTodas(java.util.List.of(
                notificacionService.crear(
                        destinatario.getUsuario(),
                        destinatario,
                        NotificacionService.TIPO_INVITACION_RECIBIDA,
                        mensaje,
                        entidadTipo,
                        entidadId,
                        proyecto.getIdProyecto())));
    }
}
