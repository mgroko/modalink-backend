package org.mgroko.backend.actividades.servicio;

import java.util.List;
import java.util.Map;

import org.mgroko.backend.actividades.dto.AsignacionActividadResponse;
import org.mgroko.backend.actividades.dto.AsignarActividadRequest;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoPermitidaException;
import org.mgroko.backend.actividades.exception.AsignacionNoDisponibleException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-70: asigna directamente una actividad a un miembro activo del proyecto.
 *
 * <p>La operación revalida la disponibilidad contra el rango factible CPM y
 * crea la {@link AsignacionActividad}. Si el perfil tiene la profesión de un
 * requerimiento de la actividad y todavía hay cupo, la asignación se vincula a
 * ese requerimiento; si no hay coincidencia, se asigna igualmente sin
 * requerimiento.</p>
 *
 * <p>La notificación {@code ACTIVIDAD_ASIGNADA} la genera el trigger
 * {@code trg_notificar_asignacion_actividad}; este servicio no la duplica.</p>
 */
@Service
public class AsignarActividadService {

    public static final String PERMISO_ASIGNAR_ACTIVIDAD = "ASIGNAR_ACTIVIDAD";

    private final PerfilRepository perfilRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final CalendarioService calendarioService;

    public AsignarActividadService(
            PerfilRepository perfilRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService,
            CalendarioService calendarioService) {
        this.perfilRepository = perfilRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.calendarioService = calendarioService;
    }

    @Transactional
    public AsignacionActividadResponse asignar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            AsignarActividadRequest request) {

        validarPerfilActor(idUsuario, idPerfilActivo);

        // Se bloquea la planificación para serializar asignaciones concurrentes
        // del mismo proyecto y no sobrepasar el cupo de un requerimiento.
        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Proyecto proyecto = planificacion.getProyecto();

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_ASIGNAR_ACTIVIDAD);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);
        validarEstadoProyecto(proyecto);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        validarEstadoActividad(actividad);

        if (planificacion.getFechaEntrega() == null) {
            throw new FechaEntregaNoDefinidaException();
        }

        Perfil perfilDestino = perfilRepository.findByIdConProfesion(request.idPerfil())
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilDestino.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil del miembro no se encuentra activo.");
        }

        if (perfilDestino.getUsuario() == null
                || perfilDestino.getUsuario().getEstado() != EstadoUsuario.Activo) {
            throw new AsignacionActividadNoPermitidaException(
                    "El usuario del miembro no se encuentra activo.");
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, perfilDestino.getIdPerfil())
                .orElseThrow(() -> new AsignacionActividadNoPermitidaException(
                        "El perfil no es miembro activo del proyecto."));

        if (asignacionActividadRepository
                .existsByMiembro_IdMiembroAndActividad_IdActividad(
                        miembro.getIdMiembro(), actividad.getIdActividad())) {
            throw new AsignacionActividadNoPermitidaException(
                    "El miembro ya está asignado a esta actividad.");
        }

        ResultadoDisponibilidad disponibilidad = verificarDisponibilidad(
                perfilDestino, actividad);

        if (!disponibilidad.disponible()) {
            throw new AsignacionNoDisponibleException(disponibilidad);
        }

        RequerimientoActividad requerimiento = resolverRequerimiento(
                actividad, perfilDestino, miembro);

        AsignacionActividad asignacion = asignacionActividadRepository.saveAndFlush(
                AsignacionActividad.builder()
                        .miembro(miembro)
                        .actividad(actividad)
                        .requerimientoActividad(requerimiento)
                        .build());

        boolean requerimientoSatisfecho = requerimiento != null
                && cupoRequerimientoService.calcularActividad(requerimiento).satisfecho();

        return new AsignacionActividadResponse(
                asignacion.getIdAsignacionAct(),
                proyecto.getIdProyecto(),
                actividad.getIdActividad(),
                miembro.getIdMiembro(),
                perfilDestino.getIdPerfil(),
                perfilDestino.getUsuario().getIdUsuario(),
                requerimiento != null ? requerimiento.getIdRequerimiento() : null,
                requerimientoSatisfecho,
                disponibilidad.provisional(),
                disponibilidad.inicioSeleccionado(),
                disponibilidad.finSeleccionado(),
                disponibilidad.resultadoPreferencia(),
                disponibilidad.desvioMinutos());
    }

    private void validarPerfilActor(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
            throw new AsignacionActividadNoPermitidaException(
                    "El proyecto no admite asignaciones en estado "
                            + estado.getNombre() + ".");
        }
    }

    private void validarEstadoActividad(Actividad actividad) {
        if (actividad.getEstado() == EstadoActividad.CANCELADA
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            throw new AsignacionActividadNoPermitidaException(
                    "La actividad no admite asignaciones en estado "
                            + actividad.getEstado().name() + ".");
        }
    }

    private ResultadoDisponibilidad verificarDisponibilidad(
            Perfil perfilDestino,
            Actividad actividad) {

        try {
            return calendarioService.verificarDisponibilidad(
                    perfilDestino.getUsuario().getIdUsuario(), actividad);
        } catch (AgendaNoEncontradaException ex) {
            throw new AsignacionActividadNoPermitidaException(
                    "El miembro no tiene agenda configurada para verificar disponibilidad.");
        }
    }

    /**
     * Resuelve el requerimiento que cubre la asignación.
     *
     * <p>Los directores se asignan sin consumir cupo. Para un miembro se toma
     * el primer requerimiento de la actividad con su misma profesión que aún
     * tenga faltantes, en orden de identificador.</p>
     */
    private RequerimientoActividad resolverRequerimiento(
            Actividad actividad,
            Perfil perfilDestino,
            MiembroProyecto miembro) {

        if (RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR) {
            return null;
        }

        if (perfilDestino.getProfesion() == null
                || perfilDestino.getProfesion().getIdProfesion() == null) {
            return null;
        }

        Long idProfesion = perfilDestino.getProfesion().getIdProfesion();
        List<RequerimientoActividad> candidatos =
                requerimientoActividadRepository
                        .findConProfesionByActividadId(actividad.getIdActividad())
                        .stream()
                        .filter(requerimiento -> requerimiento.getProfesion() != null
                                && idProfesion.equals(
                                        requerimiento.getProfesion().getIdProfesion()))
                        .toList();

        if (candidatos.isEmpty()) {
            return null;
        }

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularActividades(candidatos);

        for (RequerimientoActividad candidato : candidatos) {
            CupoRequerimiento cupo = cupos.get(candidato.getIdRequerimiento());
            if (cupo != null && cupo.faltantes() > 0) {
                return candidato;
            }
        }

        return null;
    }
}
