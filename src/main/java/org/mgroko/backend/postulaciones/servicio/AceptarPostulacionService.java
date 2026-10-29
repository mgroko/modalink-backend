package org.mgroko.backend.postulaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.RangoFactibleAbsoluto;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.dto.AdvertenciaSobrecompromiso;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-34: aceptar postulaciones a requerimientos generales y de actividad.
 */
@Service
public class AceptarPostulacionService {

    static final String PERMISO_GESTIONAR_POSTULACIONES = "GESTIONAR_POSTULACIONES";
    static final String ROL_MIEMBRO = "MIEMBRO";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RolProyectoRepository rolProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final CalendarioService calendarioService;
    private final ActividadRepository actividadRepository;
    private final CronogramaService cronogramaService;
    private final Clock reloj;

    public AceptarPostulacionService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RolProyectoRepository rolProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            CalendarioService calendarioService,
            ActividadRepository actividadRepository,
            CronogramaService cronogramaService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.calendarioService = calendarioService;
        this.actividadRepository = actividadRepository;
        this.cronogramaService = cronogramaService;
        this.reloj = reloj;
    }

    @Transactional
    public PostulacionGestionadaResponse aceptarGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        Proyecto proyecto = cargarProyectoAutorizado(
                idUsuario, idPerfilActivo, idProyecto);

        PostulacionGral postulacion = postulacionGralRepository
                .findByIdParaGestion(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimientoGral().getProyecto().getIdProyecto()
                .equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        if (postulacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new PostulacionYaGestionadaException();
        }

        Long idRequerimiento = postulacion.getRequerimientoGral().getIdRequerimientoGral();
        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findByIdParaGestion(idRequerimiento)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento general " + idRequerimiento + " no existe."));

        if (!requerimiento.getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);
        if (cupo.satisfecho()) {
            throw new PostulacionNoPermitidaException(
                    "El requerimiento de personal ya está satisfecho.");
        }

        Perfil postulante = postulacion.getPerfil();
        if (postulante.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException(
                    "El perfil postulante no se encuentra activo.");
        }

        if (miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        idProyecto,
                        postulante.getIdPerfil(),
                        EstadoParticipacion.ACTIVO)) {
            throw new PostulacionNoPermitidaException(
                    "El postulante ya es miembro activo de este proyecto.");
        }

        MiembroProyecto miembro = crearMiembro(proyecto, postulante, requerimiento);

        postulacion.setEstado(EstadoSolicitud.ACEPTADA);
        postulacion = postulacionGralRepository.saveAndFlush(postulacion);

        notificarAceptacion(postulante, proyecto, postulacion);

        return new PostulacionGestionadaResponse(
                postulacion.getIdPostulacionGral(),
                TipoPostulacion.GENERAL,
                postulacion.getEstado(),
                proyecto.getIdProyecto(),
                requerimiento.getIdRequerimientoGral(),
                postulante.getIdPerfil(),
                miembro.getIdMiembro(),
                null);
    }

    @Transactional
    public PostulacionGestionadaResponse aceptarActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idPostulacion) {

        Proyecto proyecto = cargarProyectoAutorizado(
                idUsuario, idPerfilActivo, idProyecto);

        PostulacionActividad postulacion = postulacionActividadRepository
                .findByIdParaGestion(idPostulacion)
                .orElseThrow(PostulacionNoEncontradaException::new);

        if (!postulacion.getRequerimiento().getActividad().getPlanificacion()
                .getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        if (postulacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new PostulacionYaGestionadaException();
        }

        Long idRequerimiento = postulacion.getRequerimiento().getIdRequerimiento();
        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdParaGestion(idRequerimiento)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad " + idRequerimiento + " no existe."));

        if (!requerimiento.getActividad().getPlanificacion()
                .getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new PostulacionNoEncontradaException();
        }

        if (requerimiento.getActividad().getEstado() != EstadoActividad.PENDIENTE) {
            throw new PostulacionNoPermitidaException(
                    "Sólo se puede aceptar postulaciones de actividades pendientes.");
        }

        CupoRequerimiento cupo = cupoRequerimientoService.calcularActividad(requerimiento);
        if (cupo.satisfecho()) {
            throw new PostulacionNoPermitidaException(
                    "El requerimiento de actividad ya está satisfecho.");
        }

        Perfil postulante = postulacion.getPerfil();
        if (postulante.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException(
                    "El perfil postulante no se encuentra activo.");
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, postulante.getIdPerfil())
                .orElse(null);

        if (miembro != null
                && RolProyectoUsuario.desde(miembro.getRolProyecto())
                        == RolProyectoUsuario.DIRECTOR) {
            throw new PostulacionNoPermitidaException(
                    "Un director no puede postularse a una actividad.");
        }

        if (miembro != null
                && asignacionActividadRepository
                        .existsByMiembro_IdMiembroAndActividad_IdActividad(
                                miembro.getIdMiembro(),
                                requerimiento.getActividad().getIdActividad())) {
            throw new PostulacionNoPermitidaException(
                    "El postulante ya está asignado a esta actividad.");
        }

        Set<Long> idsUsuarios = new LinkedHashSet<>();
        idsUsuarios.add(postulante.getUsuario().getIdUsuario());
        asignacionActividadRepository
                .findActivasByActividadId(requerimiento.getActividad().getIdActividad())
                .forEach(asignacion -> {
                    if (asignacion.getMiembro() != null
                            && asignacion.getMiembro().getPerfil() != null
                            && asignacion.getMiembro().getPerfil().getUsuario() != null) {
                        idsUsuarios.add(asignacion.getMiembro().getPerfil()
                                .getUsuario().getIdUsuario());
                    }
                });

        ResultadoDisponibilidad disponibilidad = calendarioService
                .verificarDisponibilidadMiembros(
                        List.copyOf(idsUsuarios),
                        requerimiento.getActividad());

        if (!disponibilidad.disponible()) {
            throw new PostulacionNoPermitidaException(
                    "No hay disponibilidad común suficiente dentro del rango factible de la actividad.");
        }

        List<AdvertenciaSobrecompromiso> advertencias = calcularAdvertencias(
                postulante,
                proyecto,
                requerimiento.getActividad(),
                disponibilidad);

        if (miembro == null) {
            miembro = crearMiembro(proyecto, postulante, null);
        }

        AsignacionActividad asignacion = asignacionActividadRepository.saveAndFlush(
                AsignacionActividad.builder()
                        .miembro(miembro)
                        .actividad(requerimiento.getActividad())
                        .requerimientoActividad(requerimiento)
                        .build());

        postulacion.setEstado(EstadoSolicitud.ACEPTADA);
        postulacion = postulacionActividadRepository.saveAndFlush(postulacion);

        notificarAceptacionActividad(
                postulante, proyecto, requerimiento.getActividad(), postulacion);
        notificarAjusteHorario(proyecto, requerimiento.getActividad(), disponibilidad);
        notificarSobrecompromiso(proyecto, postulante, requerimiento.getActividad(), advertencias);

        return new PostulacionGestionadaResponse(
                postulacion.getIdPostulacion(),
                TipoPostulacion.ACTIVIDAD,
                postulacion.getEstado(),
                proyecto.getIdProyecto(),
                idRequerimiento,
                postulante.getIdPerfil(),
                miembro.getIdMiembro(),
                asignacion.getIdAsignacionAct(),
                disponibilidad.resultadoPreferencia(),
                disponibilidad.desvioMinutos(),
                advertencias);
    }

    /**
     * Advierte, sin bloquear, si el mismo profesional tiene otras actividades
     * aceptadas sin horario persistido cuyas ventanas CPM se superponen con la
     * actividad que se está aceptando.
     */
    private List<AdvertenciaSobrecompromiso> calcularAdvertencias(
            Perfil postulante,
            Proyecto proyecto,
            Actividad objetivo,
            ResultadoDisponibilidad disponibilidad) {

        if (disponibilidad.inicioEvaluado() == null
                || disponibilidad.finEvaluado() == null) {
            return List.of();
        }

        List<AdvertenciaSobrecompromiso> advertencias = new ArrayList<>();
        for (AsignacionActividad otra : asignacionActividadRepository
                .findActivasSinHorarioDeUsuarioEnOtrosProyectos(
                        postulante.getUsuario().getIdUsuario(),
                        proyecto.getIdProyecto(),
                        objetivo.getIdActividad())) {

            Actividad actividad = otra.getActividad();
            Planificacion planificacion = actividad != null
                    ? actividad.getPlanificacion()
                    : null;
            if (planificacion == null || planificacion.getFechaEntrega() == null) {
                continue;
            }

            try {
                CalculoCpm calculo = cronogramaService.calcularProyeccion(
                        planificacion,
                        actividadRepository.findConPredecesorasByPlanificacion(
                                planificacion.getIdPlanificacion()));
                RangoFactibleAbsoluto rango = cronogramaService.rangoFactibleAbsoluto(
                        planificacion, calculo, actividad.getIdActividad());

                boolean seSuperpone = disponibilidad.inicioEvaluado().isBefore(rango.fin())
                        && disponibilidad.finEvaluado().isAfter(rango.inicio());
                if (seSuperpone) {
                    advertencias.add(new AdvertenciaSobrecompromiso(
                            actividad.getIdActividad(),
                            actividad.getNombre(),
                            planificacion.getProyecto().getIdProyecto(),
                            planificacion.getProyecto().getNombre(),
                            rango.inicio(),
                            rango.fin()));
                }
            } catch (RuntimeException ex) {
                // Un cronograma inconsistente no debe impedir la aceptación.
            }
        }
        return advertencias;
    }

    private void notificarAjusteHorario(
            Proyecto proyecto,
            Actividad actividad,
            ResultadoDisponibilidad disponibilidad) {

        if (disponibilidad.desvioMinutos() == null
                || disponibilidad.desvioMinutos() == 0) {
            return;
        }

        String mensaje = "La actividad '" + actividad.getNombre()
                + "' del proyecto '" + proyecto.getNombre()
                + "' se asignó con un desvío de " + disponibilidad.desvioMinutos()
                + " minutos respecto del horario de preferencia.";

        registrarNotificacionDirectores(
                proyecto,
                NotificacionService.TIPO_AJUSTE_HORARIO,
                mensaje,
                "ACTIVIDAD",
                actividad.getIdActividad());
    }

    private void notificarSobrecompromiso(
            Proyecto proyecto,
            Perfil postulante,
            Actividad actividad,
            List<AdvertenciaSobrecompromiso> advertencias) {

        for (AdvertenciaSobrecompromiso advertencia : advertencias) {
            String mensaje = "Posible sobrecompromiso de " + postulante.getNombreArtistico()
                    + " al aceptar la actividad '" + actividad.getNombre()
                    + "': se superpone con la actividad '"
                    + advertencia.nombreActividad()
                    + "' del proyecto '" + advertencia.nombreProyecto() + "'.";

            registrarNotificacionDirectores(
                    proyecto,
                    NotificacionService.TIPO_SOBRECOMPROMISO,
                    mensaje,
                    "ACTIVIDAD",
                    actividad.getIdActividad());
        }
    }

    private void registrarNotificacionDirectores(
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

        List<Notificacion> notificaciones = new ArrayList<>();
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
                    entidadId));
        }
        notificacionService.registrarTodas(notificaciones);
    }

    private Proyecto cargarProyectoAutorizado(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfilDirector = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilDirector.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_GESTIONAR_POSTULACIONES);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        validarEstadoProyecto(proyecto);

        return proyecto;
    }

    private MiembroProyecto crearMiembro(
            Proyecto proyecto,
            Perfil postulante,
            RequerimientoGralProyecto requerimientoGral) {

        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo(ROL_MIEMBRO)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_MIEMBRO));

        return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(postulante)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .fechaAlta(LocalDateTime.now(reloj))
                .requerimientoGral(requerimientoGral)
                .build());
    }

    private void notificarAceptacionActividad(
            Perfil postulante,
            Proyecto proyecto,
            org.mgroko.backend.modelo.Actividad actividad,
            PostulacionActividad postulacion) {

        if (postulante.getUsuario() == null) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                postulante.getUsuario(),
                postulante,
                NotificacionService.TIPO_POSTULACION_ACEPTADA,
                "Tu postulación a la actividad '" + actividad.getNombre()
                        + "' del proyecto '" + proyecto.getNombre()
                        + "' fue aceptada.",
                "POSTULACION_ACTIVIDAD",
                postulacion.getIdPostulacion());

        notificacionService.registrarTodas(List.of(notificacion));
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new PostulacionNoPermitidaException(
                    "El proyecto no permite aceptar postulaciones en estado "
                            + proyecto.getEstado().getNombre() + ".");
        }
    }

    private void notificarAceptacion(
            Perfil postulante,
            Proyecto proyecto,
            PostulacionGral postulacion) {

        if (postulante.getUsuario() == null) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                postulante.getUsuario(),
                postulante,
                NotificacionService.TIPO_POSTULACION_ACEPTADA,
                "Tu postulación al proyecto '" + proyecto.getNombre()
                        + "' fue aceptada.",
                "POSTULACION_GRAL",
                postulacion.getIdPostulacionGral());

        notificacionService.registrarTodas(List.of(notificacion));
    }
}
