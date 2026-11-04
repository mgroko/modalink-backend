package org.mgroko.backend.actividades.servicio;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.dto.DecisionIntegranteRequest;
import org.mgroko.backend.actividades.dto.EliminarActividadRequest;
import org.mgroko.backend.actividades.dto.MiembroExclusivoResponse;
import org.mgroko.backend.actividades.enums.AccionIntegrante;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.InvitacionAfectada;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.PostulacionAfectada;
import org.mgroko.backend.actividades.exception.ActividadEnCursoConfirmadaNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.DecisionIntegranteInvalidaException;
import org.mgroko.backend.actividades.exception.EliminacionIntegranteNoPermitidaException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteEliminarActividadesException;
import org.mgroko.backend.actividades.exception.MotivoEliminacionObligatorioException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.exception.MotivoEliminacionIntegranteObligatorioException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarActividadService {

    public static final String PERMISO_ELIMINAR_ACTIVIDAD = "ELIMINAR_ACTIVIDAD";

    private static final int MOTIVO_MAX_LENGTH = 200;
    private static final int MOTIVO_BAJA_MAX_LENGTH = 100;

    private final ActividadRepository actividadRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final RequerimientoActividadService requerimientoActividadService;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final MiembrosActividadService miembrosActividadService;
    private final EliminarIntegranteService eliminarIntegranteService;
    private final CronogramaService cronogramaService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final ApplicationEventPublisher eventPublisher;

    public EliminarActividadService(
            ActividadRepository actividadRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            RequerimientoActividadService requerimientoActividadService,
            RequerimientoActividadRepository requerimientoActividadRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            MiembrosActividadService miembrosActividadService,
            EliminarIntegranteService eliminarIntegranteService,
            CronogramaService cronogramaService,
            ReprogramarProyectoService reprogramarProyectoService,
            ApplicationEventPublisher eventPublisher) {
        this.actividadRepository = actividadRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.requerimientoActividadService = requerimientoActividadService;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.miembrosActividadService = miembrosActividadService;
        this.eliminarIntegranteService = eliminarIntegranteService;
        this.cronogramaService = cronogramaService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            EliminarActividadRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));
        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_ELIMINAR_ACTIVIDAD);
        validarEstadoProyecto(proyecto);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        validarEstadoActividad(proyecto, actividad);
        String motivoNormalizado = normalizarMotivo(
                request != null ? request.motivo() : null);

        List<MiembroExclusivoResponse> exclusivos =
                miembrosActividadService.listarExclusivos(
                        idUsuario, idPerfilActivo, idProyecto, idActividad);

        List<DecisionIntegranteRequest> decisiones = validarDecisiones(
                request, exclusivos, motivoNormalizado);

        aplicarDecisiones(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                motivoNormalizado,
                decisiones);

        // Se recargan después de aplicar las bajas para que el evento refleje
        // solamente a los integrantes que continúan activos en el proyecto.
        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findByActividadIdConMiembroPerfil(idActividad);
        List<PostulacionActividad> postulaciones = postulacionActividadRepository
                .findByActividadIdConPerfil(idActividad);
        List<InvitacionActividad> invitaciones = invitacionActividadRepository
                .findByActividadIdConDestinatario(idActividad);

        ActividadEliminadaEvent evento = new ActividadEliminadaEvent(
                idActividad,
                proyecto.getIdProyecto(),
                actividad.getNombre(),
                proyecto.getNombre(),
                motivoNormalizado,
                perfilesAsignados(asignaciones),
                postulacionesAfectadas(postulaciones),
                invitacionesAfectadas(invitaciones));

        ActividadDependencias.reconectarSucesoras(actividadRepository, actividad, actividades);
        actividadRepository.deleteDependenciasByActividadId(idActividad);
        actividadRepository.deleteHitoActividadByActividadId(idActividad);

        // Las filas hijas deben desaparecer antes que los requerimientos por
        // las FKs de postulacion_actividad, invitacion_actividad y
        // asignacion_actividad. El flush fuerza ese orden; no alcanza con
        // cambiar el orden de las llamadas porque Hibernate puede reordenar
        // los DELETE dentro de un mismo flush.
        postulaciones.forEach(postulacionActividadRepository::delete);
        invitaciones.forEach(invitacionActividadRepository::delete);
        asignaciones.forEach(asignacionActividadRepository::delete);
        asignacionActividadRepository.flush();

        eliminarRequerimientos(idActividad);
        actividadRepository.delete(actividad);
        actividadRepository.flush();

        if (proyecto.getEstado() != null
                && proyecto.getEstado().permiteReprogramacion()
                && planificacion.getFechaEntrega() != null) {
            reprogramarProyectoService.reprogramar(
                    idProyecto,
                    MotivoReprogramacion.ELIMINACION_ACTIVIDAD,
                    null);
        }

        publicarEvento(evento);
    }

    private List<DecisionIntegranteRequest> validarDecisiones(
            EliminarActividadRequest request,
            List<MiembroExclusivoResponse> exclusivos,
            String motivoActividad) {

        if (request == null || request.decisiones() == null) {
            throw new DecisionIntegranteInvalidaException(
                    "La lista de decisiones sobre los integrantes es obligatoria.");
        }

        Map<Long, MiembroExclusivoResponse> porId = new LinkedHashMap<>();
        for (MiembroExclusivoResponse miembro : exclusivos) {
            porId.put(miembro.idMiembro(), miembro);
        }

        Set<Long> idsDeclarados = new HashSet<>();
        for (DecisionIntegranteRequest decision : request.decisiones()) {
            if (decision == null
                    || decision.idMiembro() == null
                    || decision.accion() == null) {
                throw new DecisionIntegranteInvalidaException(
                        "Cada decisión debe indicar el miembro y la acción.");
            }

            if (!idsDeclarados.add(decision.idMiembro())) {
                throw new DecisionIntegranteInvalidaException(
                        "El miembro " + decision.idMiembro()
                                + " está repetido en las decisiones.");
            }

            MiembroExclusivoResponse miembro = porId.get(decision.idMiembro());
            if (miembro == null) {
                throw new DecisionIntegranteInvalidaException(
                        "El miembro " + decision.idMiembro()
                                + " no participa exclusivamente de esta actividad.");
            }

            if (decision.accion() == AccionIntegrante.ELIMINAR) {
                if (!miembro.puedeEliminar()) {
                    throw new EliminacionIntegranteNoPermitidaException(
                            miembro.motivoNoEliminable());
                }
                normalizarMotivoBaja(decision.motivoBaja(), motivoActividad);
            }
        }

        for (MiembroExclusivoResponse miembro : exclusivos) {
            if (!idsDeclarados.contains(miembro.idMiembro())) {
                throw new DecisionIntegranteInvalidaException(
                        "Falta la decisión para el miembro " + miembro.idMiembro() + ".");
            }
        }

        return request.decisiones();
    }

    private void aplicarDecisiones(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String motivoActividad,
            List<DecisionIntegranteRequest> decisiones) {

        for (DecisionIntegranteRequest decision : decisiones) {
            if (decision.accion() != AccionIntegrante.ELIMINAR) {
                // GUARDAR: la membresía no se modifica. La asignación a la
                // actividad se elimina junto con la actividad.
                continue;
            }

            String motivoBaja = normalizarMotivoBaja(
                    decision.motivoBaja(), motivoActividad);

            eliminarIntegranteService.eliminar(
                    idUsuario,
                    idPerfilActivo,
                    idProyecto,
                    decision.idMiembro(),
                    new EliminarIntegranteRequest(motivoBaja));
        }
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoPermiteEliminarActividadesException(estado.name());
        }
    }

    private void validarEstadoActividad(Proyecto proyecto, Actividad actividad) {
        EstadoActividad estado = actividad.getEstado();
        if (estado == EstadoActividad.FINALIZADA
                || estado == EstadoActividad.CANCELADA) {
            throw new ActividadNoEliminableException(estado.name());
        }

        if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO
                && estado == EstadoActividad.EN_CURSO) {
            throw new ActividadEnCursoConfirmadaNoEliminableException();
        }
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new MotivoEliminacionObligatorioException();
        }

        String normalizado = motivo.trim();
        if (normalizado.length() > MOTIVO_MAX_LENGTH) {
            throw new MotivoEliminacionObligatorioException(
                    "El motivo de eliminación no puede superar los "
                            + MOTIVO_MAX_LENGTH + " caracteres.");
        }

        return normalizado;
    }

    private String normalizarMotivoBaja(String motivoBaja, String motivoActividad) {
        String candidato = motivoBaja == null || motivoBaja.isBlank()
                ? motivoActividad
                : motivoBaja.trim();

        if (candidato == null || candidato.isBlank()) {
            throw new MotivoEliminacionIntegranteObligatorioException();
        }

        if (candidato.length() > MOTIVO_BAJA_MAX_LENGTH) {
            throw new MotivoEliminacionIntegranteObligatorioException(
                    "El motivo de baja del integrante no puede superar los "
                            + MOTIVO_BAJA_MAX_LENGTH
                            + " caracteres. Indicá un motivo propio más corto.");
        }

        return candidato;
    }

    private List<Long> perfilesAsignados(List<AsignacionActividad> asignaciones) {
        Set<Long> idsPerfiles = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getMiembro().getEstadoParticipacion()
                    == EstadoParticipacion.ACTIVO) {
                idsPerfiles.add(asignacion.getMiembro().getPerfil().getIdPerfil());
            }
        }
        return List.copyOf(idsPerfiles);
    }

    private List<PostulacionAfectada> postulacionesAfectadas(
            List<PostulacionActividad> postulaciones) {

        Map<Long, Long> idsPorPerfil = new LinkedHashMap<>();
        for (PostulacionActividad postulacion : postulaciones) {
            if (postulacion.getEstado() == EstadoSolicitud.RECHAZADA) {
                continue;
            }
            idsPorPerfil.putIfAbsent(
                    postulacion.getPerfil().getIdPerfil(),
                    postulacion.getIdPostulacion());
        }

        return idsPorPerfil.entrySet().stream()
                .map(entry -> new PostulacionAfectada(entry.getValue(), entry.getKey()))
                .toList();
    }

    private List<InvitacionAfectada> invitacionesAfectadas(
            List<InvitacionActividad> invitaciones) {

        Map<Long, Long> idsPorPerfil = new LinkedHashMap<>();
        for (InvitacionActividad invitacion : invitaciones) {
            if (invitacion.getEstado() == EstadoSolicitud.RECHAZADA) {
                continue;
            }
            idsPorPerfil.putIfAbsent(
                    invitacion.getPerfilDestinatario().getIdPerfil(),
                    invitacion.getIdInvitacion());
        }

        return idsPorPerfil.entrySet().stream()
                .map(entry -> new InvitacionAfectada(entry.getValue(), entry.getKey()))
                .toList();
    }

    private void eliminarRequerimientos(Long idActividad) {
        List<RequerimientoActividad> requerimientos = requerimientoActividadRepository
                .findByActividad_IdActividad(idActividad);

        for (RequerimientoActividad requerimiento : requerimientos) {
            requerimientoActividadService.eliminar(requerimiento);
        }

        if (!requerimientos.isEmpty()) {
            requerimientoActividadRepository.flush();
        }
    }

    private void recalcularCronograma(Planificacion planificacion) {

        if (planificacion.getFechaEntrega() == null) {
            return;
        }

        List<Actividad> restantes = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());

        if (restantes.isEmpty()) {
            return;
        }

        cronogramaService.recalcularTolerandoDesborde(planificacion, restantes);
    }

    private void publicarEvento(ActividadEliminadaEvent evento) {
        if (evento.idsPerfilesAsignados().isEmpty()
                && evento.postulaciones().isEmpty()
                && evento.invitaciones().isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(evento);
    }
}
